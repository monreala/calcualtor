package org.example;
import com.example.core.ExpressionEvaluator;

public class Main {
    public static void main(String[] args) {
        System.out.println("calculator");

        String[] testExpressions = {
                "-10 + 2 * 6",
                "100 * 2 + 12",
                "100 * ( 2 + 12 ) / 14",
                "(5+5) * (10 - 2)",
                "10 / 0",
                "10 + (2 * 3",
                "10 + * 5",
                "5 * (-3)"
        };

        for (String expression : testExpressions) {
            System.out.print("Virazenie " + expression + " --> ");
            try {
                int result = ExpressionEvaluator.evaluate(expression);
                System.out.println(result);
            } catch (Exception e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        }
    }
}