package org.example;

import javax.naming.LimitExceededException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;

public class RateLimitingDecorator implements ExternalApiService
{
    private ExternalApiService service;
    private Semaphore semaphore;
    private int maxRequestCount; // допустимое число запросов
    private long maxTimeInterval; // Временной интервал
    private Queue<Long> queue_requests = new ConcurrentLinkedQueue<>(); // Очередь запросов
    private Choice MyChoice; // либо выбросить исключение либо блокировать потоки
    private boolean isBlock_Semaphore = false; //Флаг для допуска потока в семафор

    public RateLimitingDecorator(ExternalApiService service, int maxThreads,
                                 int maxRequestCount, long maxTimeInterval, Choice MyChoice)
    {
        this.service = service;
        this.semaphore = new Semaphore(maxThreads);
        this.maxRequestCount = maxRequestCount;
        this.maxTimeInterval = maxTimeInterval;
        this.MyChoice = MyChoice;
    }

    @Override
    public String callExternalApi(String request) throws Exception
    {
        if(MyChoice == Choice.block)
        {
            semaphore.acquire();
            isBlock_Semaphore = true;
        }
        else
        {
            isBlock_Semaphore = semaphore.tryAcquire();
            if (!isBlock_Semaphore)
            {
                throw new LimitExceededException("Лимит потоков превышен");
            }
        }
        try
        {
            long now = System.currentTimeMillis();
            synchronized (queue_requests)
            {
                Check_Limit(now);

                if(queue_requests.size() >= maxRequestCount)
                {
                    if (MyChoice == Choice.Exception)
                    {
                        throw new LimitExceededException("Лимит запросов превышен");
                    }
                    else
                    {
                        while(queue_requests.size() >= maxRequestCount)
                        {
                            long first = queue_requests.peek();
                            long wait = maxTimeInterval - (now - first);
                            if(wait > 0)
                            {
                                Thread.sleep(wait);
                            }
                            now = System.currentTimeMillis();
                            Check_Limit(now);
                        }
                    }
                }
                queue_requests.add(now);
            }
            return service.callExternalApi(request);
        }
        finally
        {
            semaphore.release();
        }
    }

    public void Check_Limit(long time_interval)
    {
        while(!queue_requests.isEmpty())
        {
            long is = queue_requests.peek();
            if(time_interval - is> maxTimeInterval)
            {
                queue_requests.poll();
            }
            else
            {
                break;
            }
        }
    }
}


