package org.arithmetic.parser.tree;

import java.util.HashMap;
import java.util.List;
import org.arithmetic.parser.operators.ExpOperator;
import org.printer.tech.TreePrinter;

public class ExpNode {

    //Типы узлов
    public static final int NONE = 1;
    public static final int VARIABLE = 1;
    public static final int NUMBER = 2;
    public static final int OPERATOR = 3;

    private ExpNode leftOperand;
    private ExpNode rightOperand;
    private ExpOperator operator;
    private int type;
    private double value;
    private String variableName;
    // Словарь переменных, общий для всех узлов
    private HashMap<String, Double> variables;

    //Узел-оператор
    public ExpNode(ExpOperator operator, ExpNode leftOperand, ExpNode rightOperand, HashMap<String, Double> vars) {
        this.leftOperand = leftOperand;
        this.rightOperand = rightOperand;
        this.operator = operator;
        this.variables = vars;
        type = OPERATOR;
    }

    //Узел-число
    public ExpNode(double v, HashMap<String, Double> vars) {
        value = v;
        variables = vars;
        type = NUMBER;
    }

    //Узел-переменная
    public ExpNode(String name, HashMap<String, Double> vars) {
        variableName = name;
        variables = vars;
        type = VARIABLE;
			vars.putIfAbsent(variableName, 0.0);
    }

    public ExpNode getLeftOperand() {
        return leftOperand;
    }

    public void setLeftOperand(ExpNode leftOperand) {
        this.leftOperand = leftOperand;
    }

    public ExpNode getRightOperand() {
        return rightOperand;
    }

    public void setRightOperand(ExpNode rightOperand) {
        this.rightOperand = rightOperand;
    }

    public void setOperator(ExpOperator operator) {
        this.operator = operator;
    }

    public String getOperatorSign() {
		if (type == OPERATOR) {
			return operator.operatorSign();
		} else {
			return "Type is not operator";
		}
    }

    public int getType() {
        return type;
    }

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public double getValue() {
        switch (type) {
            case NUMBER:
                return value;
            case VARIABLE:
                return variables.get(variableName);
            default: // Если оператор
				if (rightOperand != null) {
					return operator.operate(leftOperand.getValue(), rightOperand.getValue());
				} else {
					return operator.operate(leftOperand.getValue(), 0.0); // Унарный оператор
				}
        }
    }

    public boolean compare() {
        return operator.compare(leftOperand.getValue(), rightOperand.getValue());
    }

    public double getComparisonDelta() {
      if (compare()) return Math.abs(leftOperand.getValue() - rightOperand.getValue());
      return leftOperand.getValue() - rightOperand.getValue();
    }

    public boolean isComparison() {
        return operator.isCompareOperator();
    }

    public boolean isAssignment() {
        return operator.isAssignment();
    }

    public void doAssignment() {
        variables.put(leftOperand.getVariableName(), rightOperand.getValue());
    }

    public List<String> getVariableNames(List<String> variableNames) {
		if (this.type == VARIABLE) {
			variableNames.add(this.variableName);
		}
		if (getLeftOperand() != null) {
			getLeftOperand().getVariableNames(variableNames);
		}
		if (getRightOperand() != null) {
			getRightOperand().getVariableNames(variableNames);
		}
        return variableNames;
    }

    public String getLabel(ExpNode this) {
        switch (type) {
            case NUMBER:
                return String.format("%.2f", value);
            case VARIABLE:
                return String.format("%s(%.2f)", variableName, variables.get(variableName));
            default:
				if (this.isComparison()) {
					return String.format("[%s](%b)", getOperatorSign(), compare());
				} else if (this.isAssignment()) {
					return String.format("[%s]", getOperatorSign());
				} else {
					return String.format("[%s](%.2f)", getOperatorSign(), getValue());
				}
        }
    }

    public String pasteVarValues(String expRepresentation, List<String> processedVars) {
        StringBuilder sb = new StringBuilder(50);
        if (this.type == VARIABLE && variables.get(variableName) != 0 && !processedVars.contains(variableName)) {
            processedVars.add(variableName);
            expRepresentation = expRepresentation.replace(variableName, String.format("%s(%.2f)", variableName, variables.get(variableName)));
        }
        sb.setLength(0);
		if (getLeftOperand() != null) {
			expRepresentation = getLeftOperand().pasteVarValues(expRepresentation, processedVars);
		}
		if (getRightOperand() != null) {
			expRepresentation = getRightOperand().pasteVarValues(expRepresentation, processedVars);
		}
        return expRepresentation;
    }


    public HashMap<String, Double> getVariables() {
        return this.variables;
    }

    public void setVariables(HashMap<String, Double> variables) {
        this.variables = variables;
    }

    public String getTreeString() {
        TreePrinter<ExpNode> printer = new TreePrinter<>(ExpNode::getLabel, ExpNode::getLeftOperand, ExpNode::getRightOperand);
        printer.setHspace(4);
        printer.setSquareBranches(true);
        return printer.treeToString(this);
    }
}