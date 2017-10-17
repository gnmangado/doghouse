package doghouse.api

class State {

    Integer id
    String name
    String code
    Date dateCreated
    Date lastUpdated

    static hasMany = [cities: City]

    static constraints = {
        name blank: false, nullable: false
        code blank: false, nullable: false
    }

    static mapping = {
        table 'state'
        version false

        id column: 'id'
        id type: 'int'

        dateCreated column: 'date_created'

        lastUpdated column: 'last_updated'

        cities column: 'state_id'
    }


    @Override
    public String toString() {
        return "State{" +
                "name='" + name + '\'' +
                '}';
    }
}
