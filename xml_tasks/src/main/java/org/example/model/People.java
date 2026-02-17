package org.example.model;

import jakarta.xml.bind.annotation.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "people")
@Data
public class People {
    @XmlAttribute(name = "count")
    private Integer count;

    @XmlElement(name = "person")
    private final List<Person> person = new ArrayList<>();
}
