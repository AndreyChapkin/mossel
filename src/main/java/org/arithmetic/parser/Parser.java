package org.arithmetic.parser;

import java.util.HashMap;
import org.arithmetic.parser.exception.ParserException;
import org.arithmetic.parser.operators.OperatorCollection;
import org.arithmetic.parser.tree.ExpNode;

public class Parser {
	
  //Типы ошибок 
  final int SYNTAX = 0; 
  final int UNBALPARENS = 1; 
  final int NOEXP = 2; 
  final int DIVBYZERO = 3;
 
  // Конец выражения
  final String EOE = "\0"; 
 
  private String exp;   // Выражение  
  private int curExpPos;   // Текущая позиция в выражении  
  private String token; // Текущая лексема  
  private int tokType;  // Тип текущей лексемы  
  
  // Словарь переменных  
  private HashMap<String, Double> vars;
  
  // Конструктор с получением внешнего словаря переменных
  public Parser(HashMap<String, Double> extVars){
	  vars = extVars;
  }
  
  public Parser(){
	  vars = new HashMap<>();
  }
 
  // Метод для использования парсера  
  public ExpNode extractTree(String expstr) throws ParserException
  {      
    curExpPos = 0;   
    exp = expstr;
    getToken();  
    if(token.equals(EOE)) 
      handleErr(NOEXP); // Пустое выражение  

    ExpNode resultTree = evalAssignment();	// Составить дерево выражения  
  
    if(!token.equals(EOE)) // Последняя лексема должна быть концом выражения
    	handleErr(SYNTAX);  
  
    return resultTree;  
  }  
    
  // Выполнение присваивания  
  private ExpNode evalAssignment() throws ParserException
  {    
    int tempTokenType;  
    String tempToken;  
  
    if(tokType == ExpNode.VARIABLE) {  
      // Сохранить старую лексему - имя переменной
      tempToken = token;  
      tempTokenType = tokType;    
      getToken();  
      if(!token.equals("=")) {  
        putBack(); // Вернуть назад считанную лексему 
        token = tempToken;  // Вернуть имя переменной в текущую лексему
        tokType = tempTokenType;  
      }  
      else {
        getToken();		// Считать значение переменной
        ExpNode resultTree = new ExpNode(OperatorCollection.getExpOperator("="),
        		new ExpNode(tempToken, vars), evalSum(), vars);
        vars.put(tempToken, resultTree.getRightOperand().getValue());
        return resultTree;  
      }  
    }  
    return evalComparison();  
  }
  //Формирование узла-неравенства 
  private ExpNode evalComparison() throws ParserException
  { 
	  
	ExpNode resultNode = evalSum();	// Создание левого операнда
	switch(token) {
		case "<":
		case "<=":
		case ">":
		case ">=":
		case "==":
		case "!=":
		      resultNode = new ExpNode(OperatorCollection.getExpOperator(token), resultNode, null, vars);
		      getToken();
		      resultNode.setRightOperand(evalSum());  // Установить правый операнд
	}  
    return resultNode;
  }  
 
  // Формирование узла-оператора сложения  
  private ExpNode evalSum() throws ParserException
  { // Создание левого операнда (возможного результата, если нет дальнейшей операции)
	ExpNode resultNode = evalMultip();	
 
    while( token.charAt(0) == '+' || token.charAt(0) == '-') {
      resultNode = new ExpNode(OperatorCollection.getExpOperator(token), resultNode, null, vars);
      getToken();
      resultNode.setRightOperand(evalMultip());  // Установить правый операнд
    }  
    return resultNode; 
  }  
    
  // Формирование узла-оператора умножения  
  private ExpNode evalMultip() throws ParserException
  { 
	ExpNode resultNode = evalPow();	// Создание левого операнда (возможного результата, если нет дальнейшей операции)
    while( token.charAt(0) == '*' || token.charAt(0) == '/' || token.charAt(0) == '%') {
      resultNode = new ExpNode(OperatorCollection.getExpOperator(token), resultNode, null, vars);
      getToken();
      resultNode.setRightOperand(evalPow());  // Установить правый операнд
      //if((resultNode.getOperatorSign().equals("/") || resultNode.getOperatorSign().equals("%")) 
    	//	  && resultNode.getRightOperand().getValue() == 0.0)	// Если правый операнд дает значение 0
    	//  handleErr(DIVBYZERO);
    }  
    return resultNode;
  }  
    
  // Формирование узла-оператора степени  
  private ExpNode evalPow() throws ParserException
  { 
	  
	ExpNode resultNode = evalUnaryOper();	// Создание левого операнда (возможного результата, если нет дальнейшей операции)
	if(token.equals("^")) {
      resultNode = new ExpNode(OperatorCollection.getExpOperator(token), resultNode, null, vars);
      getToken();
      resultNode.setRightOperand(evalUnaryOper());  // Установить правый операнд
    }  
    return resultNode;
  }  
    
  // Получение узла-унарного оператора  
  private ExpNode evalUnaryOper() throws ParserException
  { 
    String  op;  
    op = "";  
    if((tokType == ExpNode.OPERATOR) &&  
        (token.equals("+") || token.equals("-"))) {  
      op = token;  
      getToken();  
    }  
    ExpNode resultNode = evalBrackets();  
    // Если оператор - унарный минус, то вернется узел-оператор унарный минус
    if(op.equals("-")) resultNode = new ExpNode(OperatorCollection.getExpOperator("-1"), resultNode, null, vars);
    return resultNode;  
  }  
    
  // Получить узел-оператор из скобок  
  private ExpNode evalBrackets() throws ParserException
  {  
	ExpNode resultNode; 
 
    if(token.equals("(")) {  
      getToken();  
      resultNode = evalSum();  
      if(!token.equals(")"))  
        handleErr(UNBALPARENS);  
      getToken();  
    }
    else resultNode = atom();  
 
    return resultNode; 
  }  
    
  // Get the value of a number or variable.  
  private ExpNode atom() throws ParserException
  {  
	ExpNode resultNode = null; 
 
    switch(tokType) {  
      case ExpNode.NUMBER:  
        try {  
        	resultNode = new ExpNode(Double.parseDouble(token), vars);  
        } catch (NumberFormatException exc) {  
          handleErr(SYNTAX);  
        }  
        getToken();  
        break; 
      case ExpNode.VARIABLE:  
    	resultNode = new ExpNode(token, vars);  
        getToken();  
        break;  
      default:  
        handleErr(SYNTAX);  
    }  
    return resultNode; 
  }  
  
  // Возвращение лексемы обратно в выражение  
  private void putBack()    
  {  
    if(token.equals(EOE)) return;
    for(int i=0; i < token.length(); i++) curExpPos--;  
  }  
  
  // Обработка ошибки  
  private void handleErr(int error) throws ParserException
  {  
    String[] err = {  
      "Syntax Error",  
      "Unbalanced Parentheses",  
      "No Expression Present",  
      "Division by Zero",
    };  
  
    throw new ParserException(err[error]);
  }  
    
  // Obtain the next token.  
  private void getToken()  
  {  
    tokType = ExpNode.NONE;  
    token = "";  
     
    // Проверка достижения конца выражения  
    if(curExpPos == exp.length()) { 
      token = EOE; 
      return; 
    } 
    
    // Пропустить пробелы 
    while(curExpPos < exp.length() &&  
      Character.isWhitespace(exp.charAt(curExpPos))) ++curExpPos;  
  
    // Достигнут ли конец выражения после пропуска пробелов 
    if(curExpPos == exp.length()) { 
      token = EOE; 
      return; 
    } 
  
    if(isDelim(exp.charAt(curExpPos))) { // Лексема является оператором
      token += exp.charAt(curExpPos++);
      if("<>=!".contains(token) && "=".indexOf(exp.charAt(curExpPos))!=-1)
    	  token += exp.charAt(curExpPos++); //Для двухзначных операторов сравнения
      tokType = ExpNode.OPERATOR;  
    }  
    else if(Character.isLetter(exp.charAt(curExpPos))) { // Лексема является переменной 
      while(!isDelim(exp.charAt(curExpPos))) {  
        token += exp.charAt(curExpPos);  
        curExpPos++;  
        if(curExpPos >= exp.length()) break;  
      }  
      tokType = ExpNode.VARIABLE;  
    }  
    else if(Character.isDigit(exp.charAt(curExpPos))) { // Лексема является числом  
      while(!isDelim(exp.charAt(curExpPos))) {  
        token += exp.charAt(curExpPos);  
        curExpPos++;
        if(curExpPos >= exp.length()) break;  
      }  
      tokType = ExpNode.NUMBER;  
    }  
    else { // неизвестный символ
      System.out.println("ОШИБКА ОБРАБОТКИ ВЫРАЖЕНИЯ: неизвестный символ в позиции "+(curExpPos+1)+" выражения");
      token = EOE;
    }
  }  
    
  // Return true if c is a delimiter.  
  private boolean isDelim(char c)  
  {  
    if(" +-/*%^=()<>!".indexOf(c) != -1)
      return true;  
    return false;  
  }  
}


