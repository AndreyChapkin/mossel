package org.owl.parser.configuration;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.jena.ontology.Individual;
import org.arithmetic.parser.tree.ExpNode;

@Data
@AllArgsConstructor
public class FunctionalRestriction {

  private String equationBody;
  private List<Individual> arguments;
  private ExpNode expression;

  public String getEquationBody() {
    return expression.pasteVarValues(equationBody, new ArrayList<>());
  }

}
