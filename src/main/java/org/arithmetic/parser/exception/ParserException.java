package org.arithmetic.parser.exception;

public class ParserException extends Exception {
  String errStr;

  public ParserException(String str) {
    errStr = str;
  }

  public String toString() {
    return errStr;
  }
}
