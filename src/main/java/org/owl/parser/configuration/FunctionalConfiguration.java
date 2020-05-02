package org.owl.parser.configuration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.UndeclaredThrowableException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.arithmetic.parser.Parser;
import org.arithmetic.parser.exception.ParserException;
import org.arithmetic.parser.tree.ExpNode;


@Data
@Slf4j
public class FunctionalConfiguration {

  Individual funcConf; // Индивид конфигурации
  List<FunctionalRestriction> restrictions;    // Ограничения данной конфигурации
  List<FunctionalConfigurationComponent> components;  // Список компонентов функциональной конфигураций

  HashMap<String, Double> vars;  // Список переменных для ограничений

  public FunctionalConfiguration(String ontFilename, String baseNs, String funcConfName) {
    this.vars = new HashMap<>();
    this.components = new ArrayList<>();
    this.restrictions = new ArrayList<>();
    OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
    model.read("file:///" + ontFilename, null);
    this.funcConf = model.getIndividual(baseNs + "#" + funcConfName);

    // Заполнение ограничений и словаря переменных
    Parser parser = new Parser(vars);
    Property hasFunctionalRestriction = model.getProperty(baseNs + "#hasFunctionalRestriction");
    StmtIterator hasFuncRestrictionIter = funcConf.listProperties(hasFunctionalRestriction);    // Список свойств индивида
    Property hasEquationBody = model.getProperty(baseNs + "#hasEquationBody");
    Property hasArgument = model.getProperty(baseNs + "#hasArgument");
    List<FunctionalRestriction> functionalRestrictions = hasFuncRestrictionIter == null ?
      new ArrayList<>()
      : hasFuncRestrictionIter.toList().stream()
        .map(property -> {
          Individual functionalRestrictionIndividual = property.getObject().as(Individual.class);
          String restrictionRepresentation = functionalRestrictionIndividual.getProperty(hasEquationBody).getObject().asLiteral().toString();
          StmtIterator hasArgumentIter = functionalRestrictionIndividual.listProperties(hasArgument);
          List<Individual> argumentIndividuals = hasArgumentIter.toList().stream()
            .map(stmt -> stmt.getObject().as(Individual.class))
            .collect(Collectors.toList());
          ExpNode restrictExpression;
          try {
            restrictExpression = parser.extractTree(restrictionRepresentation);
          } catch (ParserException e) {
            throw new UndeclaredThrowableException(e);
          }
          return new FunctionalRestriction(restrictionRepresentation, argumentIndividuals, restrictExpression);
        }).collect(Collectors.toList());
    restrictions.addAll(functionalRestrictions);
    // Заполнение компонентов
    Property consistOf = model.getProperty(baseNs + "#consistOf");
    hasFuncRestrictionIter = funcConf.listProperties(consistOf);
    List<FunctionalConfigurationComponent> functionalConfigurationComponents = hasFuncRestrictionIter == null ?
      new ArrayList<>()
      : hasFuncRestrictionIter.toList().stream()
        .map(stmt -> new FunctionalConfigurationComponent(stmt.getObject().as(Individual.class), vars, baseNs))
        .collect(Collectors.toList());
    components.addAll(functionalConfigurationComponents);
    components.forEach(component -> restrictions.addAll(component.getRestrictions()));
  }

  @Deprecated
  public static String getXmlBase(String ontFilename) {
    String curLine = null;
    try (BufferedReader br = new BufferedReader(
      new InputStreamReader(Files.newInputStream(Paths.get(ontFilename))))) {
      while ((curLine = br.readLine()) != null) {
        if (curLine.contains("xml:base")) {
          break;
        }
      }
    } catch (Exception e) {
    }
    if (curLine != null) {
      Matcher matcher = Pattern.compile("xml:base=\"(.*)\"").matcher(curLine);
      if (matcher.find()) {
        curLine = matcher.group(1);
      }
    }
    return curLine;
  }

  // Возвращает переменные с их индивидами
  @Deprecated
  public HashMap<String, Individual> initVarsValues(String ontWithIndividualsName, String baseNs) {
    Model oldModel = this.getFuncConf().getOntModel();  // Модель с конфигурацией
    // Модель с подставляемыми индивидами
    OntModel individualsModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
    individualsModel.read("file:///" + ontWithIndividualsName, null);
    oldModel.add(individualsModel); // Сливаем модели
    HashMap<String, Double> vars = this.getVars();
    List<FunctionalConfigurationComponent> components = this.getComponents();
    // Сопоставление реальных объектов с их переменными
    HashMap<String, Individual> productVars = new HashMap<>();
    Property hasDomain = oldModel.getProperty(baseNs + "#hasDomain");
    Property hasFeature = oldModel.getProperty(baseNs + "#hasFeature");
    Property hasFeatureValue = oldModel.getProperty(baseNs + "#hasFeatureValue");
    for (FunctionalConfigurationComponent fcc : components) { // Функциональные компоненты
      Individual funcConfComponent = fcc.getFuncConfComponent(); // Текущий функциональный компонент
      // Домен компонента
      // getObject равен индивиду _class:<CLASS_NAME> - искусственные объекты-классы
      if (funcConfComponent.getProperty(hasDomain) == null) {
        log.error(String.format("Incorrect functional component: %s - property hasDomain is absent", funcConfComponent.getLocalName()));
        continue;
      }
      OntClass domain = funcConfComponent.getProperty(hasDomain).getObject().as(Individual.class).getOntClass();
      // Предполагается, что имеется по одному реальному изделию в каждом домене (классе изделий)
      ExtendedIterator<? extends OntResource> extendedIterator = domain.listInstances();
      if (!extendedIterator.hasNext()) {
        continue;  // Нет изделий
      }

      Individual product = null;
      // _class. - имеют в названии мета-объекты классов
      while (extendedIterator.hasNext() && (product = extendedIterator.next().as(Individual.class)).getLocalName().contains("_class.")) ;
      if (product == null) {
        continue;
      }

      // Характеристики изделия
      StmtIterator productFeatures = product.listProperties(hasFeature);
      while (productFeatures.hasNext()) { // Итерируемся по свойствам объекта
        // Объект-характеристика
        Individual productFeature = productFeatures.next().getObject().as(Individual.class);
        try {
          Double featureValue = Double.valueOf(productFeature.getProperty(hasFeatureValue).getString());
          String varName = String.join(".", funcConfComponent.getLocalName(), productFeature.getOntClass().getLocalName());
          vars.put(varName, featureValue);
          productVars.put(varName, product);
        } catch (Exception e) {
          log.error(String.format("%s has no number format - %s", productFeature.getLocalName(),
            productFeature.getPropertyValue(hasFeatureValue).asLiteral().toString()));
        }
      }
    }
    for (FunctionalRestriction fr : this.getRestrictions()) {
      if (fr.getExpression().isAssignment()) {
        fr.getExpression().doAssignment();
      }
    }
    /*for (FunctionalConfigurationComponent fcc : this.getComponents()) {
      for (FunctionalRestriction fr : fcc.getRestrictions()) {
        if (fr.getExpression().isAssignment()) {
          fr.getExpression().doAssignment();
        }
      }
    }*/
    return productVars;
  }

  /**
   * Метод извлекает сущности, подходящие под домены функциональных компонентов конфигурации.
   *
   * @param ontologyNameAndBaseNs 2 текстовых аргумента: имя файла онтологии и базовый ns онтологии. Если параметра не 2, то извлекаются сущности из онтологии
   *                              самой конфигурации.
   * @return словарь типа класс онтологии -> список индивидов.
   */
  public Map<OntClass, List<Individual>> extractAppropriateIndividuals(String... ontologyNameAndBaseNs) {
    Model oldModel = this.getFuncConf().getOntModel();  // Модель с конфигурацией
    String baseNs = this.getFuncConf().getNameSpace();

    if (ontologyNameAndBaseNs.length == 2
      && ontologyNameAndBaseNs[0] != null && ontologyNameAndBaseNs[1] != null) {
      // Модель с подставляемыми индивидами
      OntModel individualsModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
      individualsModel.read("file:///" + ontologyNameAndBaseNs[0], null);
      oldModel.add(individualsModel); // Сливаем модели
      baseNs = ontologyNameAndBaseNs[1];  // Переписываем ns
    }

    Property hasDomain = oldModel.getProperty(baseNs, "hasDomain");
    return this.components.stream()
      .filter(component -> {
        if (component.getFuncConfComponent().getProperty(hasDomain) != null) {
          return true;
        } else {
          log.error(String.format("Incorrect functional component: %s - property hasDomain is absent", component.getFuncConfComponent().getLocalName()));
          return false;
        }
      })
      .map(component -> {
        Individual funcComponent = component.getFuncConfComponent();
        // Домен компонента
        // getObject равен индивиду _class:<CLASS_NAME> - искусственные объекты-классы
        OntClass domain = funcComponent.getProperty(hasDomain).getObject().as(Individual.class).getOntClass();
        List<Individual> individuals = domain.listInstances().toList().stream().map(ontResource -> ontResource.as(Individual.class))
          .collect(Collectors.toList());
        return new AbstractMap.SimpleEntry<>(domain, individuals);
      })
      .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
  }

  public void initVarsValues(Map<OntClass, List<Individual>> products) {
    Model oldModel = this.getFuncConf().getOntModel();  // Модель с конфигурацией
    String baseNs = this.getFuncConf().getNameSpace();
    // Число объектов-аргументов должно совпасть с числом переменных в vars
    Map<String, Individual> arguments = new HashMap<>(20);
    this.getRestrictions().stream()
      .flatMap(restriction -> restriction.getArguments().stream())
      .forEach(argument -> arguments.put(argument.getLocalName(), argument));
    List<FunctionalConfigurationComponent> components = this.getComponents();
    Property hasFeature = oldModel.getProperty(baseNs, "hasFeature");
    Property hasFeatureValue = oldModel.getProperty(baseNs, "hasFeatureValue");
    Property featureOwner = oldModel.getProperty(baseNs, "featureOwner");
    Property fromFeature = oldModel.getProperty(baseNs, "fromFeature");
    arguments.keySet().stream().forEach(key -> {
      if (!this.vars.containsKey(key)) {
        return;
      }
      OntClass featureOwnerClass = arguments.get(key).getProperty(featureOwner).getObject().as(Individual.class).getOntClass(); // Объект _class.<Имя_класса>
      OntClass featureClass = arguments.get(key).getProperty(fromFeature).getObject().as(Individual.class).getOntClass(); // Объект _class.<Имя_класса>
      Double featureValue = null;
      OntClass featureOwnerClassKey = products.keySet().stream()
        .filter(ontClass -> ontClass.equals(featureOwnerClass))
        .findFirst()  // Должен быть один
        .orElse(null);
      if (featureOwnerClassKey == null) return;
      Individual featureOwnerProduct = products.get(featureOwnerClassKey).get(0);  // Пока что по одному индивиду
      if (featureOwnerProduct != null) {
        Individual productFeature = featureOwnerProduct.listProperties(hasFeature).toList().stream()
          .map(hasFeatureProperty -> hasFeatureProperty.getObject().as(Individual.class))
          .filter(feature -> feature.getOntClass().equals(featureClass))
          .findFirst()// Должен быть один
          .orElse(null);
        if (productFeature != null) {
          featureValue = productFeature.getPropertyValue(hasFeatureValue).asLiteral().getDouble();
        }
      }
      if (featureValue != null) {
        this.vars.put(key, featureValue);
      }
    });
  }

  // Возвращает ограничения и их отклонения
  public LinkedHashMap<FunctionalRestriction, Double> checkRestrictionsWithIndividuals(Map<OntClass, List<Individual>> products) {
    initVarsValues(products); // Проинициализировать переменные конфигурации
    // Список неудовлетворенных ограничений и их отклонений
    LinkedHashMap<FunctionalRestriction, Double> restrictionDelta = new LinkedHashMap<>();
    // Проинициализировать все присваивания
    List<FunctionalRestriction> comparisons = new ArrayList<>(20);
    List<FunctionalRestriction> assignments = new ArrayList<>(20);  // Присваивания и флаги их выполненности
    this.getRestrictions().forEach(restriction -> {
      ExpNode expression = restriction.getExpression();
      if (expression.isAssignment()) {
        assignments.add(restriction);
      } else if (expression.isComparison()) comparisons.add(restriction);
    });
    int i = 0;
    int initialCount = assignments.size();
    AtomicBoolean allAssign = new AtomicBoolean(false);
    while (i++ < initialCount && !allAssign.get()) {
      allAssign.set(true);
      for (int j = 0; j < assignments.size(); j++) {
        FunctionalRestriction assignment = assignments.get(j);
        AtomicBoolean allArgsAreComputed = new AtomicBoolean(true);
          /*assignment.getArguments().stream()
            // Все аргументы подсчитаны и отличны от 0
            .forEach(argument -> allArgsAreComputed.set(allArgsAreComputed.get() && vars.get(argument.getLocalName()) != 0));*/
        assignment.getExpression().getRightOperand().getVariableNames(new ArrayList<>()).stream()
          .forEach(varName -> allArgsAreComputed.set(allArgsAreComputed.get() && vars.get(varName) != 0));
        if (assignments.contains(assignment) && allArgsAreComputed.get()) {
          assignment.getExpression().doAssignment();
          assignments.remove(assignment);  // Выражение посчитано
          j--;
        }
        allAssign.set(allAssign.get() && assignments.size() == 0);
      }
    }
    assignments.stream().forEach(assignment -> {
      assignment.getExpression().doAssignment();
    }); // Если затесались аргументы, равные 0 по данным из словаря.

    comparisons.forEach(restriction -> {
      ExpNode expression = restriction.getExpression();
      if (expression.compare()) {
        restrictionDelta.put(restriction, expression.getComparisonDelta());  // Если выполняется ограничение, то 0 отклонение
      } else {
        restrictionDelta.put(restriction, expression.getComparisonDelta());
      }
    });
    return restrictionDelta;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(100);
    sb.append("Конфигурация '").append(funcConf.getLocalName()).append("':\n");
    sb.append("\tОграничения:\n");
    for (FunctionalRestriction fr : restrictions) {
      sb.append("\t\t").append(fr.getEquationBody()).append("\n");
    }
    sb.append("\tКомпоненты:\n");
    for (FunctionalConfigurationComponent fcc : components) {
      sb.append("\t\t").append(fcc);
    }
    return sb.toString();
  }
}
