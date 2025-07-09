package org.example;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main
{
    public static void main(String[] args)
    {
        ExternalApiService service = new RealExternalApiService();

        ExternalApiService rate_limiting = new RateLimitingDecorator(
                service, 20, 20, 60_000, Choice.EXCEPTION
        );
        ExecutorService executor = Executors.newFixedThreadPool(10);
        for(int i = 0; i < 100; i++)
        {
            final int index = i;
            executor.submit(() ->
            {
                try
                {
                    String result = rate_limiting.callExternalApi("Запрос:" + index);
                    System.out.println("Задача " + index + " Результат: " + result);
                }
                catch(Exception e)
                {
                    System.out.println("Задача " + index + " провалена: " + e.getMessage());

                }

            });
        }
        executor.shutdown();
    }
}