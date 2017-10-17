package com.mercadolibre.datadog

import com.mercadolibre.datadog.utils.Timer

/**
 * @author mlabarinas
 */
class MetricCollector extends javadog.MetricCollector {
    
    static def recordSimpleMetric(Closure action, String metricName, String... tags) {
        def result = takeTime(action)

        recordSimpleMetric(metricName, result.t, tags)

        result.r
    }

    static def recordCompoundMetric(Closure action, String metricName, String... tags) {
        def result = takeTime(action)

        recordCompoundMetric(metricName, result.t, tags)

        result.r
    }

    static def recordFullMetric(Closure action, String metricName, String... tags) {
        def result = takeTime(action)

        recordFullMetric(metricName, result.t, tags)

        result.r
    }

    static def takeTime(Closure action) {
        Timer timer = new Timer()
    
        def result = action()

        return [r: result, t: timer.getEllapsedTimeInMillis()]
    }

}