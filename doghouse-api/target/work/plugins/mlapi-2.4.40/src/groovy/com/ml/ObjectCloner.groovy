package com.ml

@Singleton
class ObjectCloner {

    List clone(List data) {
        if (data == null) return null

        def output = new LinkedList()
        for (def e : data) output.add(clone(e))

        output
    }

    Map clone(Map data) {
        if (data == null) return null

        def output = new HashMap(data.size())
        for (Map.Entry e : data.entrySet()) output.put(e.getKey(), clone(e.getValue()))

        output
    }

    def clone(data) {
        data
    }

}
