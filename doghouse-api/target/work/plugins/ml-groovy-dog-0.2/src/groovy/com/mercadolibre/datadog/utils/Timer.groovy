package com.mercadolibre.datadog.utils

/**
 * @author mlabarinas
 */
public class Timer {

    static final String ELAPSED_TIME = "Elapsed time: "
    private Long startTime
    private Long endTime

    public Timer() {
        start()   
    }

    public void start() {
        startTime = getCurrentTime()
    }

    public void end() {
        endTime = getCurrentTime()
    }

    public Long getEllapsedTimeInMillis() {
        if(!endTime) {
            end()
        }

        return endTime - startTime
    }

    public String getEllaspedTime() {
        if (!endTime)
            end()

        return TimeCategory.minus(new Date(endTime), new Date(startTime)).toString()
    }

    private Long getCurrentTime() {
        return System.currentTimeMillis()
    }

    @Override
    public String toString() {
        return ELAPSED_TIME + getEllapsedTimeInMillis()
    }

}