package berlin.yuna.quarkus.mongodb;

import berlin.yuna.quakrus.mongodb.model.Person;
import berlin.yuna.quarkus.mongodb.util.EmbeddedMongoDb;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
//TODO: move to a quarkus extension if possible
@QuarkusTestResource(value = EmbeddedMongoDb.class)
class PersonContractTest {

    @Test
    void createPerson() {
        System.out.println("DELETED: " + Person.deleteAll());
        System.out.println("ALL: " + Person.listAll());

        Person person = new Person();
        person.setName("MyPerson");
        person.persistOrUpdate();
        person.persistOrUpdate();
        person.persistOrUpdate();

        System.out.println("ALL: " + Person.listAll());
        System.out.println("DELETED: " + Person.deleteAll());
    }
}