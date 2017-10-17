package doghouse.api

class City {

    Integer id
    String name
    String code
    Date dateCreated
    Date lastUpdated

    State state

    static constraints = {
        name blank: false, nullable: false
        code blank: false, nullable: false
    }

    static mapping = {
        table 'city'
        version false

        id column: 'id'
        id type: 'int'

        dateCreated column: 'date_created'

        lastUpdated column: 'last_updated'

        state column: 'state_id'
    }


    @Override
    public String toString() {
        return "City{" +
                "name='" + name + '\'' +
                '}';
    }
}
