package org.example.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class Family {
    @XmlElement(name = "fatherRef") private List<Ref> fatherRef = new ArrayList<>();
    @XmlElement(name = "fatherName") private List<String> fatherName = new ArrayList<>();
    @XmlElement(name = "motherRef") private List<Ref> motherRef = new ArrayList<>();
    @XmlElement(name = "motherName") private List<String> motherName = new ArrayList<>();

    @XmlElement(name = "brotherRef") private List<Ref> brotherRef = new ArrayList<>();
    @XmlElement(name = "brotherName") private List<String> brotherName = new ArrayList<>();
    @XmlElement(name = "sisterRef") private List<Ref> sisterRef = new ArrayList<>();
    @XmlElement(name = "sisterName") private List<String> sisterName = new ArrayList<>();

    @XmlElement(name = "sonRef") private List<Ref> sonRef = new ArrayList<>();
    @XmlElement(name = "sonName") private List<String> sonName = new ArrayList<>();
    @XmlElement(name = "daughterRef") private List<Ref> daughterRef = new ArrayList<>();
    @XmlElement(name = "daughterName") private List<String> daughterName = new ArrayList<>();

    @XmlElement(name = "grandfatherRef") private List<Ref> grandfatherRef = new ArrayList<>();
    @XmlElement(name = "grandfatherName") private List<String> grandfatherName = new ArrayList<>();
    @XmlElement(name = "grandmotherRef") private List<Ref> grandmotherRef = new ArrayList<>();
    @XmlElement(name = "grandmotherName") private List<String> grandmotherName = new ArrayList<>();

    @XmlElement(name = "uncleRef") private List<Ref> uncleRef = new ArrayList<>();
    @XmlElement(name = "uncleName") private List<String> uncleName = new ArrayList<>();
    @XmlElement(name = "auntRef") private List<Ref> auntRef = new ArrayList<>();
    @XmlElement(name = "auntName") private List<String> auntName = new ArrayList<>();
}

