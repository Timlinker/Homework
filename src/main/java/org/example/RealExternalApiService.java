package org.example;

public class RealExternalApiService implements ExternalApiService
{
    @Override
    public String callExternalApi(String request) throws Exception
    {
        Thread.sleep(300);
        return "Ответ для запроса:" + request;
    }
}
