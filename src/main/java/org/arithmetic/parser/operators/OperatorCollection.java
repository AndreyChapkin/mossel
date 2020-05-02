package org.arithmetic.parser.operators;

import java.util.HashMap;

//Класс для ведения реализаций интерфейса ExpOperator - операторы
public class OperatorCollection{
	
	//Словарь операторов
	private static final HashMap<String, ExpOperator> operators = new HashMap<>();
	
	//Начальное наполнение словаря операторов +, -, *, /, %, ^, -1, =
	static {
		operators.put("+", new ExpOperator() {
			@Override
			public String operatorSign() {
				return "+";
			}
			@Override
			public double operate(double firstOperand, double secondOperand) {
				return firstOperand + secondOperand;
			}
		});
		operators.put("-", new ExpOperator() {
			@Override
			public String operatorSign() {
				return "-";
			}
			@Override
			public double operate(double firstOperand, double secondOperand) {
				return firstOperand - secondOperand;
			}
		});
		operators.put("*", new ExpOperator() {
			@Override
			public String operatorSign() {
				return "*";
			}
			@Override
			public double operate(double firstOperand, double secondOperand) {
				return firstOperand * secondOperand;
			}
		});
		operators.put("/", new ExpOperator() {
			@Override
			public String operatorSign() {
				return "/";
			}
			@Override
			public double operate(double firstOperand, double secondOperand) {
				return firstOperand / secondOperand;
			}
		});
		operators.put("%", new ExpOperator() {
			@Override
			public String operatorSign() {
				return "%";
			}
			@Override
			public double operate(double firstOperand, double secondOperand) {
				return firstOperand % secondOperand;
			}
		});
		operators.put("^", new ExpOperator() {
			@Override
			public String operatorSign() {
				return "^";
			}
			@Override
			public double operate(double firstOperand, double secondOperand) {
				return Math.pow(firstOperand, secondOperand);
			}
		});
		operators.put("-1", new ExpOperator() {
			@Override
			public String operatorSign() {
				return "-1";
			}
			@Override
			public double operate(double firstOperand, double secondOperand) {
				return -firstOperand;
			}
		});
		
		operators.put("=", new ExpOperator() {
			@Override
			public String operatorSign() {
				return "=";
			}
			public boolean isAssignment() {
				return true;
			}
		});
		operators.put("<", new ExpOperator() {
			@Override
			public String operatorSign() {
				return "<";
			}
			@Override
			public boolean compare(double firstOperand, double secondOperand) {
				return firstOperand < secondOperand;
			}
			@Override
			public boolean isCompareOperator() {
				return true;
			}
		});
		operators.put("<=", new ExpOperator() {
			@Override
			public String operatorSign() {
				return "<=";
			}
			@Override
			public boolean compare(double firstOperand, double secondOperand) {
				return firstOperand <= secondOperand;
			}
			@Override
			public boolean isCompareOperator() {
				return true;
			}
		});
		operators.put(">", new ExpOperator() {
			@Override
			public String operatorSign() {
				return ">";
			}
			@Override
			public boolean compare(double firstOperand, double secondOperand) {
				return firstOperand > secondOperand;
			}
			@Override
			public boolean isCompareOperator() {
				return true;
			}
		});
		operators.put(">=", new ExpOperator() {
			@Override
			public String operatorSign() {
				return ">=";
			}
			@Override
			public boolean compare(double firstOperand, double secondOperand) {
				return firstOperand >= secondOperand;
			}
			@Override
			public boolean isCompareOperator() {
				return true;
			}
		});
		operators.put("==", new ExpOperator() {
			@Override
			public String operatorSign() {
				return "==";
			}
			@Override
			public boolean compare(double firstOperand, double secondOperand) {
				return firstOperand == secondOperand;
			}
			@Override
			public boolean isCompareOperator() {
				return true;
			}
		});
		operators.put("!=", new ExpOperator() {
			@Override
			public String operatorSign() {
				return "!=";
			}
			@Override
			public boolean compare(double firstOperand, double secondOperand) {
				return firstOperand != secondOperand;
			}
			@Override
			public boolean isCompareOperator() {
				return true;
			}
		});
	}
	
	public static ExpOperator getExpOperator(String operSign) {
		return operators.get(operSign);
	}
	
	public static void putExpOperator(String operSign, ExpOperator eo) {
		operators.put(operSign, eo);
	}
}
