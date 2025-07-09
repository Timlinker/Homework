package org.example;

import javax.naming.LimitExceededException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

public class RateLimitingDecorator implements ExternalApiService
{
    private ExternalApiService service;
    private Semaphore semaphore;
    private int maxRequestCount; // допустимое число запросов
    private long maxTimeInterval; // Временной интервал
    private Queue<Long> QueueRequests = new ConcurrentLinkedQueue<>(); // Очередь запросов
    private Choice MyChoice; // либо выбросить исключение либо блокировать потоки

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
        boolean isBlockSemaphore = false; //Флаг для допуска потока в семафор

        if(MyChoice == Choice.BLOCK)
        {
            semaphore.acquire();
            isBlockSemaphore = true;
        }
        else
        {
            isBlockSemaphore = semaphore.tryAcquire();
            if (!isBlockSemaphore)
            {
                throw new LimitExceededException("Лимит потоков превышен");
            }
        }
        try
        {
            long now = System.currentTimeMillis();
            synchronized (QueueRequests)
            {
                Check_Limit(now);

                if(QueueRequests.size() >= maxRequestCount)
                {
                    if (MyChoice == Choice.EXCEPTION)
                    {
                        throw new LimitExceededException("Лимит запросов превышен");
                    }
                    else
                    {
                        while(QueueRequests.size() >= maxRequestCount)
                        {
                            long first = QueueRequests.peek();
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
                QueueRequests.add(now);
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
        while(!QueueRequests.isEmpty())
        {
            long is = QueueRequests.peek();
            if(time_interval - is> maxTimeInterval)
            {
                QueueRequests.poll();
            }
            else
            {
                break;
            }
        }
    }
}


