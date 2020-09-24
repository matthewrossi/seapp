package com.android.server.pm.sepolicy;

import java.util.ArrayList;
import java.util.List;

public class PolicyLexer {

    public enum Type {
        COMMENT, OPAREN, CPAREN, SYMBOL, QSTRING, UNKNOWN;
    }

    public static class Token {

        public final Type type;
        public final String value;
        public final int line;

        public Token(Type type, String value, int line) {
            this.type = type;
            this.value = value;
            this.line = line;
        }

        public String toString() {
            if(type == Type.SYMBOL) {
                return "SYMBOL<" + value + ">";
            }
            return type.toString();
        }

    }

    /*
     * Given a String, and an index, get the symbol starting at that index
     */
    private static String getSymbol(String s, int i) {
        int j = i;
        while(j < s.length()) {
            if(Character.isLetterOrDigit(s.charAt(j)) ||
                    s.charAt(j) == '[' || s.charAt(j) == ']' || s.charAt(j) == '.' || s.charAt(j) == '@' ||
                    s.charAt(j) == '=' || s.charAt(j) == '/' || s.charAt(j) == '*' || s.charAt(j) == '-' ||
                    s.charAt(j) == '_' || s.charAt(j) == '$' || s.charAt(j) == '%' || s.charAt(j) == '+' ||
                    s.charAt(j) == '-' || s.charAt(j) == '!' || s.charAt(j) == '|' || s.charAt(j) == '&' ||
                    s.charAt(j) == '^' || s.charAt(j) == ':' || s.charAt(j) == '~' || s.charAt(j) == '`' ||
                    s.charAt(j) == '#' || s.charAt(j) == '{' || s.charAt(j) == '}' || s.charAt(j) == '\'' ||
                    s.charAt(j) == '<' || s.charAt(j) == '>' || s.charAt(j) == '?' || s.charAt(j) == ',') {
                j++;
            } else {
                return s.substring(i, j);
            }
        }
        return s.substring(i, j);
    }

    public static List<Token> lex(String input, int line) {
        List<Token> tokens = new ArrayList<>();
        String value;
        int i = 0;
        while(i < input.length()) {
            switch(input.charAt(i)) {
                case '(':
                    tokens.add(new Token(Type.OPAREN, "(", line));
                    i++;
                    break;
                case ')':
                    tokens.add(new Token(Type.CPAREN, ")", line));
                    i++;
                    break;
                case ';':
                    value = input.substring(i);
                    tokens.add(new Token(Type.COMMENT, value, line));
                    i += value.length();
                    break;
                case '"':
                    int end = input.indexOf('"', i);
                    if (end != -1){
                        value = input.substring(i, end+1);
                        tokens.add(new Token(Type.QSTRING, value, line));
                        i += value.length();
                    }
                    else{
                        value = input.substring(i);
                        tokens.add(new Token(Type.UNKNOWN, value, line));
                        i += value.length();
                    }
                    break;
                default:
                    if(Character.isWhitespace(input.charAt(i))) {
                        i++;
                    } else {
                        value = getSymbol(input, i);
                        tokens.add(new Token(Type.SYMBOL, value, line));
                        i += value.length();
                    }
                    break;
            }
        }
        return tokens;
    }

}
