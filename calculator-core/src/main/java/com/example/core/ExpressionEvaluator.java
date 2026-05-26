package com.example.core;

import java.util.Stack;

public class ExpressionEvaluator {
    public static int evaluate(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            throw new IllegalArgumentException("pustoe pole");
        }
        char[] tokkens = expression.toCharArray();
        Stack<Integer> values = new Stack<>();
        Stack<Character> ops = new Stack<>();


        boolean canBeUnary = true;

        for (int i = 0; i < tokkens.length; i++) {
            if (tokkens[i] == ' ') {
                continue;
            }


            boolean isUnaryMinus = (tokkens[i] == '-' && canBeUnary);


            if (Character.isDigit(tokkens[i]) || isUnaryMinus) {
                StringBuilder sbuf = new StringBuilder();

                if (isUnaryMinus) {
                    sbuf.append('-');
                    i++;

                    while (i < tokkens.length && tokkens[i] == ' ') {
                        i++;
                    }
                }

                if (i < tokkens.length && Character.isDigit(tokkens[i])) {
                    while (i < tokkens.length && Character.isDigit(tokkens[i])) {
                        sbuf.append(tokkens[i++]);
                    }
                    i--;
                    values.push(Integer.parseInt(sbuf.toString()));
                    canBeUnary = false;
                } else {
                    throw new IllegalArgumentException("ne tot simbol posle minusa");
                }

            } else if (tokkens[i] == '(') {
                ops.push(tokkens[i]);
                canBeUnary = true;

            } else if (tokkens[i] == ')') {
                while (!ops.empty() && ops.peek() != '(') {
                    processToOperator(values, ops);
                }
                if (ops.empty() || ops.peek() != '(') {
                    throw new IllegalArgumentException("nesovpadenie scobok");
                }
                ops.pop();
                canBeUnary = false;

            } else if (tokkens[i] == '+' || tokkens[i] == '-' || tokkens[i] == '*' || tokkens[i] == '/') {
                while (!ops.empty() && hasPrecedence(tokkens[i], ops.peek())) {
                    processToOperator(values, ops);
                }
                ops.push(tokkens[i]);
                canBeUnary = true;

            } else {
                throw new IllegalArgumentException("ne tot simbol '" + tokkens[i] + "'");
            }
        }

        while (!ops.empty()) {
            if (ops.peek() == '(' || ops.peek() == ')') {
                throw new IllegalArgumentException("nesovpadenie skobok");
            }
            processToOperator(values, ops);
        }

        if (values.size() != 1) {
            throw new IllegalArgumentException("ne vernii sintacsis virajeniea");
        }

        return values.pop();
    }

    public static void processToOperator(Stack<Integer> values, Stack<Character> ops) {
        if (values.size() < 2) {
            throw new IllegalArgumentException("oshibka operatorov");
        }
        int val2 = values.pop();
        int val1 = values.pop();
        char op = ops.pop();
        values.push(applyOp(op, val1, val2));
    }

    public static int applyOp(char op, int a, int b) {
        switch (op) {
            case '+': return a + b;
            case '-': return a - b;
            case '*': return a * b;
            case '/':
                if (b == 0) {
                    throw new ArithmeticException("na noli ne delim");
                }
                return a / b;
        }
        return 0;
    }

    private static boolean hasPrecedence(char op1, char op2) {
        if (op2 == '(' || op2 == ')') {
            return false;
        }
        if ((op1 == '*' || op1 == '/') && (op2 == '+' || op2 == '-')) {
            return false;
        }
        return true;
    }
}