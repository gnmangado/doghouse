package doghouse.api

import grails.transaction.Transactional

class StateService {

    def findStates() {
        return State.getAll()
    }
}
