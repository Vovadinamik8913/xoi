package org.example;

import org.example.entity.PersonData;
import org.example.model.Gender;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class Merger {

    public Map<String, PersonData> mergePersons(List<PersonData> personList) {
        
        Map<String, PersonData> peopleOut = new HashMap<>();
        
        Map<String, List<PersonData>> idsToTemps = new HashMap<>();
        Map<String, List<Gender>> genderHints = new HashMap<>();
        Map<String, List<PersonData>> namesToTemps = new HashMap<>();
        Map<String, List<String>> namesToIds = new HashMap<>();
        
        firstPass(personList, peopleOut, idsToTemps, genderHints, namesToTemps, namesToIds);

        processIdBasedRelations(peopleOut, idsToTemps, genderHints);
        
        processNameBasedRelations(peopleOut, idsToTemps, genderHints, namesToTemps, namesToIds);
        
        resolveGenders(peopleOut, genderHints);

        distributeByGender(peopleOut);

        return peopleOut;
    }

    private void firstPass(List<PersonData> personList,
                           Map<String, PersonData> peopleOut,
                           Map<String, List<PersonData>> idsToTemps,
                           Map<String, List<Gender>> genderHints,
                           Map<String, List<PersonData>> namesToTemps,
                           Map<String, List<String>> namesToIds) {
        for (PersonData person : personList) {
            if (person.getId() != null) {
                
                if (!peopleOut.containsKey(person.getId())) {
                    peopleOut.put(person.getId(), new PersonData(person.getId()));
                    genderHints.put(person.getId(), new ArrayList<>());
                    idsToTemps.put(person.getId(), new ArrayList<>());
                }
                idsToTemps.get(person.getId()).add(person);
            } else {
                String key = getFullName(person);
                if (key != null) {
                    namesToTemps.computeIfAbsent(key, k -> new ArrayList<>()).add(person);
                }
            }
        }
        
        for (String id : peopleOut.keySet()) {
            PersonData person = peopleOut.get(id);
            String fullName = person.bestFull();
            if (fullName != null) {
                namesToIds.computeIfAbsent(fullName, k -> new ArrayList<>()).add(id);
            }
        }
    }

    private void processIdBasedRelations(Map<String, PersonData> peopleOut,
                                         Map<String, List<PersonData>> idsToTemps,
                                         Map<String, List<Gender>> genderHints) {
        BiConsumer<String, String> setHusbandWife = createSpouseHandler(peopleOut, genderHints);
        BiConsumer<String, String> addChild = createChildHandler(peopleOut);
        BiConsumer<String, String> addSibling = createSiblingHandler(peopleOut, addChild);

        for (String id : peopleOut.keySet()) {
            List<PersonData> temps = idsToTemps.get(id);
            if (temps == null) continue;

            PersonData person = peopleOut.get(id);

            for (PersonData temp : temps) {
                processIdBasedSpouse(temp, id, peopleOut, setHusbandWife);
                processIdBasedChildren(temp, id, peopleOut, addChild, genderHints);
                processIdBasedParents(temp, id, peopleOut, addChild);
                processIdBasedSiblings(temp, id, peopleOut, addSibling);
                mergeBasicInfo(person, temp);
            }
        }
    }

    private void processNameBasedRelations(Map<String, PersonData> peopleOut,
                                           Map<String, List<PersonData>> idsToTemps,
                                           Map<String, List<Gender>> genderHints,
                                           Map<String, List<PersonData>> namesToTemps,
                                           Map<String, List<String>> namesToIds) {
        BiConsumer<String, String> setHusbandWife = createSpouseHandler(peopleOut, genderHints);
        BiConsumer<String, String> addChild = createChildHandler(peopleOut);
        BiConsumer<String, String> addSibling = createSiblingHandler(peopleOut, addChild);

        for (String id : peopleOut.keySet()) {
            List<PersonData> temps = new ArrayList<>(idsToTemps.getOrDefault(id, new ArrayList<>()));
            PersonData person = peopleOut.get(id);

            
            String fullName = person.bestFull();
            if (fullName != null) {
                List<PersonData> additionals = namesToTemps.get(fullName);
                if (additionals != null) {
                    temps.addAll(additionals.stream()
                            .filter(t -> shouldMergeByName(person, t))
                            .collect(Collectors.toList()));
                }
            }

            for (PersonData temp : temps) {
                processNameBasedSpouse(temp, id, peopleOut, setHusbandWife, namesToIds, genderHints);
                processNameBasedParents(temp, id, peopleOut, addChild, namesToIds, genderHints);
                processNameBasedSiblings(temp, id, peopleOut, addSibling, namesToIds, genderHints);
                processNameBasedChildren(temp, id, peopleOut, addChild, namesToIds);
            }
        }
    }

    

    private BiConsumer<String, String> createSpouseHandler(Map<String, PersonData> peopleOut,
                                                           Map<String, List<Gender>> genderHints) {
        return (hId, wId) -> {
            if (!peopleOut.containsKey(hId) || !peopleOut.containsKey(wId)) return;

            PersonData husband = peopleOut.get(hId);
            PersonData wife = peopleOut.get(wId);

            
            if (husband.getSpouseIds() != null && !husband.getSpouseIds().isEmpty() &&
                    !husband.getSpouseIds().contains(wId)) return;
            if (wife.getSpouseIds() != null && !wife.getSpouseIds().isEmpty() &&
                    !wife.getSpouseIds().contains(hId)) return;

            husband.getSpouseIds().add(wId);
            wife.getSpouseIds().add(hId);

            
            genderHints.get(hId).add(Gender.MALE);
            genderHints.get(wId).add(Gender.FEMALE);
        };
    }

    private BiConsumer<String, String> createChildHandler(Map<String, PersonData> peopleOut) {
        return (pId, cId) -> {
            if (!peopleOut.containsKey(pId) || !peopleOut.containsKey(cId)) return;

            PersonData parent = peopleOut.get(pId);
            PersonData child = peopleOut.get(cId);

            
            if (child.getParentIds().size() >= 2) return; 

            
            if (child.getChildIds().contains(pId)) return;

            parent.getChildIds().add(cId);
            child.getParentIds().add(pId);
        };
    }

    private BiConsumer<String, String> createSiblingHandler(Map<String, PersonData> peopleOut,
                                                            BiConsumer<String, String> addChild) {
        return (lhsId, rhsId) -> {
            if (!peopleOut.containsKey(lhsId) || !peopleOut.containsKey(rhsId)) return;

            PersonData lhs = peopleOut.get(lhsId);
            PersonData rhs = peopleOut.get(rhsId);

            
            if (lhs.getChildIds().contains(rhsId) || rhs.getChildIds().contains(lhsId)) return;
            if (lhs.getParentIds().contains(rhsId) || rhs.getParentIds().contains(lhsId)) return;

            lhs.getSiblingIds().add(rhsId);
            rhs.getSiblingIds().add(lhsId);

            
            for (String parentId : lhs.getParentIds()) {
                addChild.accept(parentId, rhsId);
            }
            for (String parentId : rhs.getParentIds()) {
                addChild.accept(parentId, lhsId);
            }
        };
    }

    

    private void processIdBasedSpouse(PersonData temp, String id,
                                      Map<String, PersonData> peopleOut,
                                      BiConsumer<String, String> setHusbandWife) {
        for (String spouseId : temp.getSpouseIds()) {
            if (peopleOut.containsKey(spouseId)) {
                setHusbandWife.accept(id, spouseId);
            }
        }
    }

    private void processIdBasedChildren(PersonData temp, String id,
                                        Map<String, PersonData> peopleOut,
                                        BiConsumer<String, String> addChild,
                                        Map<String, List<Gender>> genderHints) {
        for (String sonId : temp.getSonIds()) {
            if (peopleOut.containsKey(sonId)) {
                addChild.accept(id, sonId);
                genderHints.get(sonId).add(Gender.MALE);
            }
        }
        for (String daughterId : temp.getDaughterIds()) {
            if (peopleOut.containsKey(daughterId)) {
                addChild.accept(id, daughterId);
                genderHints.get(daughterId).add(Gender.FEMALE);
            }
        }
        for (String childId : temp.getChildIds()) {
            if (peopleOut.containsKey(childId)) {
                addChild.accept(id, childId);
            }
        }
    }

    private void processIdBasedParents(PersonData temp, String id,
                                       Map<String, PersonData> peopleOut,
                                       BiConsumer<String, String> addChild) {
        for (String parentId : temp.getParentIds()) {
            if (peopleOut.containsKey(parentId)) {
                addChild.accept(parentId, id);
            }
        }
    }

    private void processIdBasedSiblings(PersonData temp, String id,
                                        Map<String, PersonData> peopleOut,
                                        BiConsumer<String, String> addSibling) {
        for (String siblingId : temp.getSiblingIds()) {
            if (peopleOut.containsKey(siblingId)) {
                addSibling.accept(id, siblingId);
            }
        }
    }


    private void processNameBasedSpouse(PersonData temp, String id,
                                        Map<String, PersonData> peopleOut,
                                        BiConsumer<String, String> setHusbandWife,
                                        Map<String, List<String>> namesToIds,
                                        Map<String, List<Gender>> genderHints) {
        for (String spouseName : temp.getSpouseNames()) {
            List<String> ids = namesToIds.get(spouseName);
            if (ids != null && !ids.isEmpty()) {
                String spouseId = findBestMatchForSpouse(id, ids, peopleOut);
                setHusbandWife.accept(id, spouseId);
                genderHints.get(spouseId).add(inferGenderFromSpouse(temp));
            }
        }
    }

    private void processNameBasedParents(PersonData temp, String id,
                                         Map<String, PersonData> peopleOut,
                                         BiConsumer<String, String> addChild,
                                         Map<String, List<String>> namesToIds,
                                         Map<String, List<Gender>> genderHints) {
        for (String parentName : temp.getParentNames()) {
            List<String> ids = namesToIds.get(parentName);
            if (ids != null && !ids.isEmpty()) {
                String parentId = findBestMatchForParent(id, ids, peopleOut);
                addChild.accept(parentId, id);

                if (temp.getFatherNames().contains(parentName)) {
                    genderHints.get(parentId).add(Gender.MALE);
                } else if (temp.getMotherNames().contains(parentName)) {
                    genderHints.get(parentId).add(Gender.FEMALE);
                }
            }
        }
    }

    private void processNameBasedSiblings(PersonData temp, String id,
                                          Map<String, PersonData> peopleOut,
                                          BiConsumer<String, String> addSibling,
                                          Map<String, List<String>> namesToIds,
                                          Map<String, List<Gender>> genderHints) {

        for (String brotherName : temp.getBrotherNames()) {
            List<String> ids = namesToIds.get(brotherName);
            if (ids != null && !ids.isEmpty()) {
                String brotherId = findBestMatchForSibling(id, ids, peopleOut);
                addSibling.accept(id, brotherId);
                genderHints.get(brotherId).add(Gender.MALE);
            }
        }

        for (String sisterName : temp.getSisterNames()) {
            List<String> ids = namesToIds.get(sisterName);
            if (ids != null && !ids.isEmpty()) {
                String sisterId = findBestMatchForSibling(id, ids, peopleOut);
                addSibling.accept(id, sisterId);
                genderHints.get(sisterId).add(Gender.FEMALE);
            }
        }

        for (String siblingName : temp.getSiblingNames()) {
            List<String> ids = namesToIds.get(siblingName);
            if (ids != null && !ids.isEmpty()) {
                String siblingId = findBestMatchForSibling(id, ids, peopleOut);
                addSibling.accept(id, siblingId);
            }
        }
    }

    private void processNameBasedChildren(PersonData temp, String id,
                                          Map<String, PersonData> peopleOut,
                                          BiConsumer<String, String> addChild,
                                          Map<String, List<String>> namesToIds) {
        for (String sonName : temp.getSonNames()) {
            List<String> ids = namesToIds.get(sonName);
            if (ids != null && !ids.isEmpty()) {
                String childId = findBestMatchForChild(id, ids, peopleOut);
                addChild.accept(id, childId);
            }
        }

        for (String daughterName : temp.getDaughterNames()) {
            List<String> ids = namesToIds.get(daughterName);
            if (ids != null && !ids.isEmpty()) {
                String childId = findBestMatchForChild(id, ids, peopleOut);
                addChild.accept(id, childId);
            }
        }

        for (String childName : temp.getChildNames()) {
            List<String> ids = namesToIds.get(childName);
            if (ids != null && !ids.isEmpty()) {
                String childId = findBestMatchForChild(id, ids, peopleOut);
                addChild.accept(id, childId);
            }
        }
    }

    

    private String findBestMatchForSpouse(String personId,
                                          List<String> candidates,
                                          Map<String, PersonData> peopleOut) {
        PersonData person = peopleOut.get(personId);

        return candidates.stream()
                .filter(candidateId -> {
                    PersonData candidate = peopleOut.get(candidateId);
                    
                    if (person.getSpouseIds().contains(candidateId)) return true;
                    
                    for (String childId : person.getChildIds()) {
                        PersonData child = peopleOut.get(childId);
                        if (child.getParentIds().contains(candidateId)) return true;
                    }
                    return false;
                })
                .findFirst()
                .orElse(candidates.get(0));
    }

    private String findBestMatchForParent(String childId,
                                          List<String> candidates,
                                          Map<String, PersonData> peopleOut) {
        PersonData child = peopleOut.get(childId);

        return candidates.stream()
                .filter(candidateId -> {
                    PersonData candidate = peopleOut.get(candidateId);
                    
                    if (child.getParentIds().contains(candidateId)) return true;
                    
                    if (candidate.getChildIds().contains(childId)) return true;
                    return false;
                })
                .findFirst()
                .orElse(candidates.get(0));
    }

    private String findBestMatchForSibling(String personId,
                                           List<String> candidates,
                                           Map<String, PersonData> peopleOut) {
        PersonData person = peopleOut.get(personId);

        return candidates.stream()
                .filter(candidateId -> {
                    PersonData candidate = peopleOut.get(candidateId);
                    
                    if (person.getSiblingIds().contains(candidateId)) return true;
                    
                    for (String parentId : person.getParentIds()) {
                        if (candidate.getParentIds().contains(parentId)) return true;
                    }
                    return false;
                })
                .findFirst()
                .orElse(candidates.get(0));
    }

    private String findBestMatchForChild(String parentId,
                                         List<String> candidates,
                                         Map<String, PersonData> peopleOut) {
        PersonData parent = peopleOut.get(parentId);

        return candidates.stream()
                .filter(candidateId -> {
                    PersonData candidate = peopleOut.get(candidateId);
                    
                    if (parent.getChildIds().contains(candidateId)) return true;
                    
                    if (candidate.getParentIds().size() < 2) return true;
                    return false;
                })
                .findFirst()
                .orElse(candidates.get(0));
    }

    

    private void mergeBasicInfo(PersonData target, PersonData src) {
        target.getFirstNames().addAll(src.getFirstNames());
        target.getLastNames().addAll(src.getLastNames());
        target.getFullNames().addAll(src.getFullNames());

        if (target.getId() == null && src.getId() != null) {
            target.setId(src.getId());
        }
    }

    private boolean shouldMergeByName(PersonData existing, PersonData candidate) {
        
        if (candidate.getId() != null && !candidate.getId().equals(existing.getId())) {
            return false;
        }

        
        if (!existing.getSpouseIds().isEmpty() && !candidate.getSpouseIds().isEmpty()) {
            
            if (!existing.getSpouseIds().equals(candidate.getSpouseIds())) {
                return false;
            }
        }

        return true;
    }

    private String getFullName(PersonData person) {
        return person.bestFull();
    }

    private Gender inferGenderFromSpouse(PersonData temp) {
        return Gender.UNKNOWN;
    }

    private void resolveGenders(Map<String, PersonData> peopleOut,
                                Map<String, List<Gender>> genderHints) {
        for (String id : genderHints.keySet()) {
            List<Gender> hints = genderHints.get(id);
            if (hints.isEmpty()) continue;

            long maleCount = hints.stream().filter(g -> g == Gender.MALE).count();
            long femaleCount = hints.stream().filter(g -> g == Gender.FEMALE).count();

            Gender resolved;
            if (maleCount > femaleCount) {
                resolved = Gender.MALE;
            } else if (femaleCount > maleCount) {
                resolved = Gender.FEMALE;
            } else {
                resolved = Gender.UNKNOWN;
            }

            peopleOut.get(id).getGenderEvidence().add(resolved);
        }
    }

    private void distributeByGender(Map<String, PersonData> peopleOut) {
        for (String id : peopleOut.keySet()) {
            PersonData person = peopleOut.get(id);

            
            for (String childId : person.getChildIds()) {
                PersonData child = peopleOut.get(childId);
                if (child != null) {
                    Gender childGender = child.resolvedGender();
                    if (childGender == Gender.MALE) {
                        person.getSonIds().add(childId);
                    } else if (childGender == Gender.FEMALE) {
                        person.getDaughterIds().add(childId);
                    }
                }
            }

            
            for (String siblingId : person.getSiblingIds()) {
                PersonData sibling = peopleOut.get(siblingId);
                if (sibling != null) {
                    Gender siblingGender = sibling.resolvedGender();
                    if (siblingGender == Gender.MALE) {
                        person.getBrotherNames().add(sibling.bestFull());
                    } else if (siblingGender == Gender.FEMALE) {
                        person.getSisterNames().add(sibling.bestFull());
                    }
                }
            }
        }
    }
}
