package org.example.model;

import jakarta.xml.bind.annotation.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@Data
@RequiredArgsConstructor
public class Person {
    @XmlAttribute(name = "id")
    @XmlID
    private final String id;

    @XmlElement(name = "name")
    private NameData nameData;

    @XmlElement(required = true)
    public Gender gender = Gender.UNKNOWN;

    @XmlElement
    @XmlIDREF
    public Person mother;

    @XmlElement
    @XmlIDREF
    public Person father;

    @XmlElement
    @XmlIDREF
    public Person husband;

    @XmlElement
    @XmlIDREF
    public Person wife;

    @XmlElementWrapper(name = "brothers")
    @XmlElement(name = "brother")
    @XmlIDREF
    public List<Person> brothers = new ArrayList<>();

    @XmlElementWrapper(name = "sisters")
    @XmlElement(name = "sister")
    @XmlIDREF
    public List<Person> sisters = new ArrayList<>();

    @XmlElementWrapper(name = "sons")
    @XmlElement(name = "son")
    @XmlIDREF
    public List<Person> sons = new ArrayList<>();

    @XmlElementWrapper(name = "daughters")
    @XmlElement(name = "daughter")
    @XmlIDREF
    public List<Person> daughters = new ArrayList<>();
}
