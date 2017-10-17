package doghouse.api

class StateController {

    def stateService

    def find() {
        log.info params

//        render(contentType: 'text/json') {[
//                'data': stateService.findStates(),
//                'status': results ? "OK" : "Nothing present"
//        ]}
        return [response: stateService.findStates(), status: 200]
    }

    def crete() {
        log.info params
        render "ALO"
    }

    def update() {
        log.info params
        render "ALO"
    }

    def get() {
        log.info params
        render "ALO"
    }
}
