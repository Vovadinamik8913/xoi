package org.example.mapper;

import org.example.entity.PersonData;
import org.example.model.Gender;
import org.example.model.NameData;
import org.example.model.People;
import org.example.model.Person;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Mapper {

    public static People toPeople(Map<String, PersonData> peopleMap) {
        var result = new People();

        Map<String, Person> personById = new HashMap<>();
        List<PersonData> sortedList = new ArrayList<>(peopleMap.values());
        sortedList.sort(Comparator.comparing(p -> p.getId() == null ? "" : p.getId()));

        for (PersonData p : sortedList) {
            if (p.getId() == null) continue;

            Person person = new Person(p.getId());

            NameData nameData = createNameData(p);
            if (nameData != null) {
                person.setNameData(nameData);
            }

            person.setGender(p.resolvedGender());

            personById.put(p.getId(), person);
        }

        for (PersonData p : sortedList) {
            if (p.getId() == null) continue;

            Person person = personById.get(p.getId());
            if (person == null) continue;

            setParents(person, p, personById);

            setSpouses(person, p, personById);

            setChildren(person, p, personById);

            setSiblings(person, p, personById);
        }

        for (PersonData p : sortedList) {
            if (p.getId() != null) {
                Person person = personById.get(p.getId());
                if (person != null) {
                    result.getPerson().add(person);
                }
            }
        }

        result.setCount(result.getPerson().size());
        return result;
    }

    private static NameData createNameData(PersonData p) {
        String full = p.bestFull();
        String first = p.bestFirst();
        String last = p.bestLast();

        if (!isMeaningful(first) && !isMeaningful(last) && !isMeaningful(full)) {
            return null;
        }

        NameData nameData = new NameData();
        if (isMeaningful(first)) nameData.setFirst(first);
        if (isMeaningful(last)) nameData.setLast(last);
        if (isMeaningful(full)) nameData.setFull(full);

        return nameData;
    }

    private static void setParents(Person person, PersonData p, Map<String, Person> personById) {
        for (String parentId : p.getParentIds()) {
            Person parent = personById.get(parentId);
            if (parent == null) continue;

            Gender parentGender = parent.getGender();
            if (parentGender == Gender.MALE) {
                person.setFather(parent);
            } else if (parentGender == Gender.FEMALE) {
                person.setMother(parent);
            }
        }
    }

    private static void setSpouses(Person person, PersonData p, Map<String, Person> personById) {
        Gender personGender = person.getGender();

        for (String spouseId : p.getSpouseIds()) {
            Person spouse = personById.get(spouseId);
            if (spouse == null) continue;

            if (personGender == Gender.MALE) {
                person.setWife(spouse);
            } else if (personGender == Gender.FEMALE) {
                person.setHusband(spouse);
            }
        }
    }

    private static void setChildren(Person person, PersonData p, Map<String, Person> personById) {
        for (String sonId : p.getSonIds()) {
            Person son = personById.get(sonId);
            if (son != null && son.getGender() == Gender.MALE) {
                person.getSons().add(son);
            }
        }

        for (String daughterId : p.getDaughterIds()) {
            Person daughter = personById.get(daughterId);
            if (daughter != null && daughter.getGender() == Gender.FEMALE) {
                person.getDaughters().add(daughter);
            }
        }

        for (String childId : p.getChildIds()) {
            Person child = personById.get(childId);
            if (child == null) continue;

            Gender childGender = child.getGender();
            if (childGender == Gender.MALE) {
                person.getSons().add(child);
            } else if (childGender == Gender.FEMALE) {
                person.getDaughters().add(child);
            }
        }
    }

    private static void setSiblings(Person person, PersonData p, Map<String, Person> personById) {
        for (String siblingId : p.getSiblingIds()) {
            Person sibling = personById.get(siblingId);
            if (sibling == null) continue;

            Gender siblingGender = sibling.getGender();
            if (siblingGender == Gender.MALE) {
                person.getBrothers().add(sibling);
            } else if (siblingGender == Gender.FEMALE) {
                person.getSisters().add(sibling);
            }
        }
    }

    private static boolean isMeaningful(String s) {
        return s != null && !s.isBlank();
    }
}