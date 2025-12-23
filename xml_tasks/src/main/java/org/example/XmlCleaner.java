package org.example;

import org.example.model.Gender;
import org.example.model.Person;

import javax.xml.stream.*;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class XmlCleaner {

    public static void main(String[] args) throws Exception {
        String inputPath = args != null && args.length > 0 ? args[0] : "src/main/resources/people.xml";
        String outputPath = args != null && args.length > 1 ? args[1] : "build/output/people_consolidated.xml";

        Path in = Path.of(inputPath);
        Path out = Path.of(outputPath);
        Files.createDirectories(out.getParent());

        Map<String, Person> people = parsePeople(in);
        writeConsolidated(out, people);

        System.out.println("Parsed people (by id/name): " + people.size());
        System.out.println("Wrote: " + out.toAbsolutePath());
    }

    private static Map<String, Person> parsePeople(Path input) throws Exception {
        XMLInputFactory f = XMLInputFactory.newInstance();
        Map<String, Person> byId = new LinkedHashMap<>();
        Map<String, Person> byName = new LinkedHashMap<>();

        try (InputStream is = new BufferedInputStream(Files.newInputStream(input))) {
            XMLStreamReader r = f.createXMLStreamReader(is);
            while (r.hasNext()) {
                int ev = r.next();
                if (ev == XMLStreamConstants.START_ELEMENT && r.getLocalName().equals("person")) {
                    Person p = readPerson(r);
                    mergePerson(p, byId, byName);
                }
            }
            r.close();
        }

        for (Iterator<Map.Entry<String, Person>> it = byName.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, Person> e = it.next();
            Person p = e.getValue();
            String keyName = e.getKey();
            Person target = findByBestName(byId, keyName);
            if (target != null) {
                mergeInto(target, p);
                it.remove();
            }
        }

        int anon = 1;
        for (Person p : byName.values()) {
            if (p.getId() == null) {
                p.setId("ANON_" + (anon++));
            }
            byId.put(p.getId(), p);
        }
        return byId;
    }

    private static Person readPerson(XMLStreamReader r) throws XMLStreamException {
        Person p = new Person();

        String attrId = attr(r, "id");
        if (isMeaningful(attrId)) p.setId(attrId);
        String attrName = attr(r, "name");
        if (isMeaningful(attrName)) p.getFullNames().add(clean(attrName));

        int depth = 1;
        while (r.hasNext() && depth > 0) {
            int ev = r.next();
            if (ev == XMLStreamConstants.START_ELEMENT) {
                String el = r.getLocalName();
                switch (el) {
                    case "id": {
                        String v = attr(r, "value");
                        if (isMeaningful(v)) p.setId(v);
                        String text = safeElementText(r); // consumes to END_ELEMENT id
                        if (isMeaningful(text)) p.setId(text);
                        break;
                    }
                    case "firstname": {
                        String v = attr(r, "value");
                        String text = v != null ? v : safeElementText(r);
                        if (isMeaningful(text)) p.getFirstNames().add(clean(text));
                        break;
                    }
                    case "surname": {
                        String v = attr(r, "value");
                        String text = v != null ? v : safeElementText(r);
                        if (isMeaningful(text)) p.getLastNames().add(clean(text));
                        break;
                    }
                    case "fullname": {
                        readFullname(r, p);
                        break;
                    }
                    case "gender": {
                        String v = attr(r, "value");
                        String text = v != null ? v : safeElementText(r);
                        Gender g = parseGender(text);
                        if (g != null) p.getGenderEvidence().add(g);
                        break;
                    }
                    case "husband":
                    case "wife":
                    case "spouse":
                    case "spouce": {
                        String v = attr(r, "value");
                        if (isMeaningful(v)) addIdOrName(v, p.getSpouseIds(), p.getSpouseNames());
                        skipElement(r);
                        break;
                    }
                    case "father": {
                        String text = safeElementText(r);
                        if (isMeaningful(text) && !isUnknown(text)) {
                            String t = clean(text);
                            p.getParentNames().add(t);
                            p.getFatherNames().add(t);
                        }
                        break;
                    }
                    case "mother": {
                        String text = safeElementText(r);
                        if (isMeaningful(text) && !isUnknown(text)) {
                            String t = clean(text);
                            p.getParentNames().add(t);
                            p.getMotherNames().add(t);
                        }
                        break;
                    }
                    case "parent": {
                        String v = attr(r, "value");
                        if (isMeaningful(v) && !isUnknown(v)) addIdOrName(v, p.getParentIds(), p.getParentNames());
                        String text = safeElementText(r);
                        if (isMeaningful(text) && !isUnknown(text)) addIdOrName(text, p.getParentIds(), p.getParentNames());
                        break;
                    }
                    case "siblings": {
                        String v = attr(r, "val");
                        if (isMeaningful(v)) {
                            for (String token : v.split("\\s+")) {
                                if (isMeaningful(token)) addIdOrName(token, p.getSiblingIds(), p.getSiblingNames());
                            }
                            skipElement(r);
                        } else {
                            readSiblingsBlock(r, p);
                        }
                        break;
                    }
                    case "siblings-number": {
                        String v = attr(r, "value");
                        String text = v != null ? v : safeElementText(r);
                        Integer n = parseIntSafe(text);
                        if (n != null) p.setExpectedSiblings(n);
                        break;
                    }
                    case "children-number": {
                        String v = attr(r, "value");
                        String text = v != null ? v : safeElementText(r);
                        Integer n = parseIntSafe(text);
                        if (n != null) p.setExpectedChildren(n);
                        break;
                    }
                    case "children": {
                        readChildrenBlock(r, p);
                        break;
                    }
                    default: {
                        skipElement(r);
                    }
                }
            } else if (ev == XMLStreamConstants.END_ELEMENT) {
                if (r.getLocalName().equals("person")) depth--;
            }
        }
        return p;
    }

    private static void readFullname(XMLStreamReader r, Person p) throws XMLStreamException {
        String first = null, last = null;
        int depth = 1;
        while (r.hasNext() && depth > 0) {
            int ev = r.next();
            if (ev == XMLStreamConstants.START_ELEMENT) {
                String el = r.getLocalName();
                if (el.equals("first")) {
                    first = safeElementText(r);
                } else if (el.equals("family")) {
                    last = safeElementText(r);
                } else {
                    skipElement(r);
                }
            } else if (ev == XMLStreamConstants.END_ELEMENT && r.getLocalName().equals("fullname")) {
                depth--;
            }
        }
        if (isMeaningful(first)) p.getFirstNames().add(clean(first));
        if (isMeaningful(last)) p.getLastNames().add(clean(last));
        String full = buildFull(first, last);
        if (isMeaningful(full)) p.getFullNames().add(full);
    }

    private static void readSiblingsBlock(XMLStreamReader r, Person p) throws XMLStreamException {
        int depth = 1;
        while (r.hasNext() && depth > 0) {
            int ev = r.next();
            if (ev == XMLStreamConstants.START_ELEMENT) {
                String el = r.getLocalName();
                if (el.equals("brother")) {
                    String text = safeElementText(r);
                    if (isMeaningful(text)) p.getBrotherNames().add(clean(text));
                } else if (el.equals("sister")) {
                    String text = safeElementText(r);
                    if (isMeaningful(text)) p.getSisterNames().add(clean(text));
                } else if (el.equals("sibling")) {
                    String text = safeElementText(r);
                    if (isMeaningful(text)) p.getSiblingNames().add(clean(text));
                } else {
                    skipElement(r);
                }
            } else if (ev == XMLStreamConstants.END_ELEMENT && r.getLocalName().equals("siblings")) {
                depth--;
            }
        }
    }

    private static void readChildrenBlock(XMLStreamReader r, Person p) throws XMLStreamException {
        int depth = 1;
        while (r.hasNext() && depth > 0) {
            int ev = r.next();
            if (ev == XMLStreamConstants.START_ELEMENT) {
                String el = r.getLocalName();
                if (el.equals("son")) {
                    String id = attr(r, "id");
                    if (isMeaningful(id)) { p.getSonIds().add(id.trim()); skipElement(r); }
                    else { String text = safeElementText(r); if (isMeaningful(text)) p.getSonNames().add(clean(text)); }
                } else if (el.equals("daughter")) {
                    String id = attr(r, "id");
                    if (isMeaningful(id)) { p.getDaughterIds().add(id.trim()); skipElement(r); }
                    else { String text = safeElementText(r); if (isMeaningful(text)) p.getDaughterNames().add(clean(text)); }
                } else if (el.equals("child")) {
                    String id = attr(r, "id");
                    if (isMeaningful(id)) { p.getChildIds().add(id.trim()); skipElement(r); }
                    else { String text = safeElementText(r); if (isMeaningful(text)) p.getChildNames().add(clean(text)); }
                } else {
                    skipElement(r);
                }
            } else if (ev == XMLStreamConstants.END_ELEMENT && r.getLocalName().equals("children")) {
                depth--;
            }
        }
    }

    private static void mergePerson(Person incoming, Map<String, Person> byId, Map<String, Person> byName) {
        String bestName = incoming.bestFull();
        if (isMeaningful(incoming.getId())) {
            Person target = byId.get(incoming.getId());
            if (target == null) {
                target = incoming;
                byId.put(target.getId(), target);
            } else {
                mergeInto(target, incoming);
            }
            if (isMeaningful(bestName)) {
                Person nameOnly = byName.remove(normalizeKey(bestName));
                if (nameOnly != null) mergeInto(target, nameOnly);
            }
        } else if (isMeaningful(bestName)) {
            String key = normalizeKey(bestName);
            Person target = byName.get(key);
            if (target == null) byName.put(key, incoming);
            else mergeInto(target, incoming);
        } else {
            String key = "__ANON__" + byName.size();
            byName.put(key, incoming);
        }
    }

    private static void mergeInto(Person target, Person src) {
        if (target.getId() == null && src.getId() != null) target.setId(src.getId());

        target.getFirstNames().addAll(src.getFirstNames());
        target.getLastNames().addAll(src.getLastNames());
        target.getFullNames().addAll(src.getFullNames());
        target.getGenderEvidence().addAll(src.getGenderEvidence());

        target.getSpouseIds().addAll(src.getSpouseIds());
        target.getSpouseNames().addAll(src.getSpouseNames());

        target.getParentIds().addAll(src.getParentIds());
        target.getParentNames().addAll(src.getParentNames());

        target.getSiblingIds().addAll(src.getSiblingIds());
        target.getSiblingNames().addAll(src.getSiblingNames());
        target.getBrotherNames().addAll(src.getBrotherNames());
        target.getSisterNames().addAll(src.getSisterNames());

        target.getSonIds().addAll(src.getSonIds());
        target.getDaughterIds().addAll(src.getDaughterIds());
        target.getChildIds().addAll(src.getChildIds());
        target.getSonNames().addAll(src.getSonNames());
        target.getDaughterNames().addAll(src.getDaughterNames());
        target.getChildNames().addAll(src.getChildNames());

        if (target.getExpectedChildren() == null) target.setExpectedChildren(src.getExpectedChildren());
        if (target.getExpectedSiblings() == null) target.setExpectedSiblings(src.getExpectedSiblings());
    }

    private static Person findByBestName(Map<String, Person> byId, String bestNameKey) {
        for (Person p : byId.values()) {
            String best = p.bestFull();
            if (isMeaningful(best) && normalizeKey(best).equals(bestNameKey)) return p;
        }
        return null;
    }

    private static void writeConsolidated(Path output, Map<String, Person> people) throws Exception {
        XMLOutputFactory of = XMLOutputFactory.newInstance();
        try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(output))) {
            XMLStreamWriter w = of.createXMLStreamWriter(os, "UTF-8");
            w.writeStartDocument("UTF-8", "1.0");
            w.writeStartElement("people");
            w.writeAttribute("count", String.valueOf(people.size()));

            List<Person> list = new ArrayList<>(people.values());
            list.sort(Comparator.comparing(p -> p.getId() == null ? "" : p.getId()));

            for (Person p : list) {
                w.writeStartElement("person");
                if (isMeaningful(p.getId())) w.writeAttribute("id", p.getId());

                String full = p.bestFull();
                String first = p.bestFirst();
                String last = p.bestLast();
                if (isMeaningful(full) || isMeaningful(first) || isMeaningful(last)) {
                    w.writeStartElement("name");
                    if (isMeaningful(first)) writeSimple(w, "first", first);
                    if (isMeaningful(last)) writeSimple(w, "last", last);
                    if (isMeaningful(full)) writeSimple(w, "full", full);
                    w.writeEndElement();
                }

                Gender g = p.resolvedGender();
                w.writeEmptyElement("gender");
                w.writeAttribute("value", genderToString(g));

                if (!p.getSpouseIds().isEmpty() || !p.getSpouseNames().isEmpty()) {
                    w.writeStartElement("spouses");
                    for (String sid : p.getSpouseIds()) { w.writeEmptyElement("spouse"); w.writeAttribute("id", sid); }
                    for (String sn : p.getSpouseNames()) writeSimple(w, "spouse", sn);
                    w.writeEndElement();
                }

                if (!p.getParentIds().isEmpty() || !p.getParentNames().isEmpty()) {
                    w.writeStartElement("parents");
                    for (String pid : p.getParentIds()) { w.writeEmptyElement("parent"); w.writeAttribute("id", pid); }
                    for (String pn : p.getParentNames()) writeSimple(w, "parent", pn);
                    w.writeEndElement();
                }

                if (!p.getSonIds().isEmpty() || !p.getDaughterIds().isEmpty() || !p.getChildIds().isEmpty() ||
                        !p.getSonNames().isEmpty() || !p.getDaughterNames().isEmpty() || !p.getChildNames().isEmpty()) {
                    w.writeStartElement("children");
                    for (String id : p.getSonIds()) { w.writeEmptyElement("son"); w.writeAttribute("id", id); }
                    for (String id : p.getDaughterIds()) { w.writeEmptyElement("daughter"); w.writeAttribute("id", id); }
                    for (String id : p.getChildIds()) { w.writeEmptyElement("child"); w.writeAttribute("id", id); }
                    for (String nm : p.getSonNames()) writeSimple(w, "son", nm);
                    for (String nm : p.getDaughterNames()) writeSimple(w, "daughter", nm);
                    for (String nm : p.getChildNames()) writeSimple(w, "child", nm);
                    w.writeEndElement();
                }

                if (!p.getSiblingIds().isEmpty() || !p.getBrotherNames().isEmpty() || !p.getSisterNames().isEmpty() || !p.getSiblingNames().isEmpty()) {
                    w.writeStartElement("siblings");
                    for (String sid : p.getSiblingIds()) {
                        Person sib = people.get(sid);
                        Gender sg = sib != null ? sib.resolvedGender() : Gender.UNKNOWN;
                        String tag = (sg == Gender.MALE) ? "brother" : (sg == Gender.FEMALE ? "sister" : "sibling");
                        w.writeEmptyElement(tag);
                        w.writeAttribute("id", sid);
                    }
                    for (String nm : p.getBrotherNames()) writeSimple(w, "brother", nm);
                    for (String nm : p.getSisterNames()) writeSimple(w, "sister", nm);
                    for (String nm : p.getSiblingNames()) writeSimple(w, "sibling", nm);
                    w.writeEndElement();
                }

                if (p.getExpectedChildren() != null || p.getExpectedSiblings() != null) {
                    w.writeEmptyElement("expected");
                    if (p.getExpectedChildren() != null) w.writeAttribute("children", String.valueOf(p.getExpectedChildren()));
                    if (p.getExpectedSiblings() != null) w.writeAttribute("siblings", String.valueOf(p.getExpectedSiblings()));
                }

                w.writeEndElement();
            }

            w.writeEndElement();
            w.writeEndDocument();
            w.flush();
            w.close();
        }
    }

    private static void writeSimple(XMLStreamWriter w, String tag, String text) throws XMLStreamException {
        w.writeStartElement(tag);
        w.writeCharacters(text);
        w.writeEndElement();
    }

    private static String attr(XMLStreamReader r, String name) {
        String v = r.getAttributeValue(null, name);
        if (v == null) {
            for (int i = 0; i < r.getAttributeCount(); i++) {
                if (name.equals(r.getAttributeLocalName(i))) return r.getAttributeValue(i);
            }
        }
        return v;
    }

    private static void addIdOrName(String raw, Set<String> ids, Set<String> names) {
        String v = raw.trim();
        if (v.matches("[Pp]\\d+")) ids.add(v.toUpperCase());
        else if (!isUnknown(v)) names.add(clean(v));
    }

    private static boolean isUnknown(String s) {
        String v = s == null ? null : s.trim();
        if (v == null || v.isEmpty()) return false;
        String t = v.toLowerCase();
        return t.equals("unknown") || t.equals("none");
    }

    private static String clean(String s) {
        if (s == null) return null;
        return s.trim().replaceAll("\\s+", " ");
    }

    private static boolean isMeaningful(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static String normalizeKey(String name) {
        return clean(name).toLowerCase();
    }

    private static String buildFull(String first, String last) {
        first = clean(first);
        last = clean(last);
        if (!isMeaningful(first) && !isMeaningful(last)) return null;
        if (!isMeaningful(first)) return last;
        if (!isMeaningful(last)) return first;
        return first + " " + last;
    }

    private static Integer parseIntSafe(String s) {
        if (!isMeaningful(s)) return null;
        try { return Integer.parseInt(s.trim()); } catch (Exception ignored) { return null; }
    }

    private static Gender parseGender(String raw) {
        if (!isMeaningful(raw)) return null;
        String s = raw.trim().toLowerCase();
        if (s.equals("m") || s.equals("male")) return Gender.MALE;
        if (s.equals("f") || s.equals("female")) return Gender.FEMALE;
        return Gender.UNKNOWN;
    }

    private static String genderToString(Gender g) {
        if (g == null) return "unknown";
        switch (g) {
            case MALE: return "male";
            case FEMALE: return "female";
            default: return "unknown";
        }
    }

    private static String safeElementText(XMLStreamReader r) throws XMLStreamException {
        try {
            return clean(r.getElementText());
        } catch (XMLStreamException ex) {
            skipElement(r);
            return null;
        }
    }

    private static void skipElement(XMLStreamReader r) throws XMLStreamException {
        int depth = 1;
        while (depth > 0 && r.hasNext()) {
            int ev = r.next();
            if (ev == XMLStreamConstants.START_ELEMENT) depth++;
            else if (ev == XMLStreamConstants.END_ELEMENT) depth--;
        }
    }
}
