package org.arithmetic.parser.operators;

//Интерфейс для реализации будущими бинарными операторами
public interface ExpOperator{
	String operatorSign();
	default double operate(double firstOperand, double secondOperand) {
		return 0.0;
	}
	default boolean compare(double firstOperand, double secondOperand){
		return false;
	}
	default boolean isCompareOperator() {
		return false;
	}
	default boolean isAssignment() {
		return false;
	}
}
