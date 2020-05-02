package org.owl.parser.configuration;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;
import org.apache.jena.ontology.Individual;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.StmtIterator;
import org.arithmetic.parser.Parser;
import org.arithmetic.parser.exception.ParserException;
import org.arithmetic.parser.tree.ExpNode;

@Data
public class FunctionalConfigurationComponent {
	Individual funcConfComponent; // Индивид конфигурации
	List<FunctionalRestriction> restrictions;		// Ограничения данной конфигурации
	HashMap<String, Double> vars;	// Список переменных для ограничений
	
	public FunctionalConfigurationComponent(Individual funcConfComponent, HashMap<String, Double> vars, String baseNs) {
		this.funcConfComponent = funcConfComponent;
		this.vars = vars;
		this.restrictions = new ArrayList<>();
		
		// Заполнение ограничений и словаря переменных
		Parser parser = new Parser(vars);
		Model model = funcConfComponent.getModel();
		Property hasFunctionalRestriction = model.getProperty(baseNs + "#hasFunctionalRestriction");
		Property hasEquationBody = model.getProperty(baseNs + "#hasEquationBody");
		Property hasArgument = model.getProperty(baseNs + "#hasArgument");
		StmtIterator hasFunctionalRestrictionIter = funcConfComponent.listProperties(hasFunctionalRestriction);
		List<FunctionalRestriction> functionalRestrictions = hasFunctionalRestrictionIter.toList().stream()
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
				} catch(ParserException e) { throw new UndeclaredThrowableException(e); }
				return new FunctionalRestriction(restrictionRepresentation, argumentIndividuals, restrictExpression);
			}).collect(Collectors.toList());
		restrictions.addAll(functionalRestrictions);
		/*while(hasFunctionalRestrictionIter.hasNext())
			try {
				String restrictionRepresentation = hasFunctionalRestrictionIter.next().getObject().asLiteral().toString();
				FunctionalRestriction restriction = new FunctionalRestriction(restrictionRepresentation, parser.evaluate(restrictionRepresentation));
				restrictions.add(restriction);
			} catch (Exception e) {e.printStackTrace();}*/
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(100);
		sb.append("'").append(funcConfComponent.getLocalName()).append("':\n");
		sb.append("\t\t\tОграничения:\n");
		for(FunctionalRestriction fr: restrictions)
			sb.append("\t\t\t\t").append(fr.getEquationBody()).append("\n");
		return sb.toString();
	}
}
