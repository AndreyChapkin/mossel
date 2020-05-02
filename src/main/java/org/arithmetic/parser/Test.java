package org.arithmetic.parser;

import java.util.HashMap;
import org.arithmetic.parser.exception.ParserException;
import org.arithmetic.parser.tree.ExpNode;

public class Test {
	
	static <T> void o(T obj) {
		System.out.print(obj.toString());
	}
	static <T> void ol(T obj) {
		System.out.println(obj.toString());
	}
	static void of(String pattern, Object... objs) {
		for(Object o: objs)
			pattern = pattern.replaceFirst("%[0-9.dscf]+", o.toString());
		System.out.print(pattern);
	}
	
	public static void main(String[] args) throws ParserException {
		HashMap<String, Double> vars = new HashMap<>();
		vars.put("a", 666.6);
		Parser p = new Parser(vars);
		ExpNode root1 = p.extractTree("2*(x-3)==-3^2-15");
		ExpNode root2 = p.extractTree("2*(y-3)==-3^2-15");
		ol(root1.getTreeString());
		System.out.println(root1.getVariables());
		ol(root2.getTreeString());
		System.out.println(vars);
	}
}
