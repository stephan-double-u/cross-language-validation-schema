# Cross Language Validation Schema - V0.2
JSON schema that specifies validation rules in a language independant manner to enable cross language validation.

An online, interactive JSON Schema validator can be found here: https://www.jsonschemavalidator.net/

## Table of Contents
* [TL;DR](#tl;dr)
* [Motivation](#motivation)
* [Documentation](#documentation)
* [Implementations](#implementations)
* [TODOs](#todos)

# TL;DR
The purpose of this _JSON Schema_ is to describe _complex validation rules_, independent of a specific 
programming language. The resulting JSON documents are intended to be used in applications that involve multiple 
components possibly written in different programmming languages where the rules have to be validated in several 
components, e.g. in a frontend written in ES6 and a backend written in Java. 

The **main objective** is to apply the [DRY principle](https://en.wikipedia.org/wiki/Don%27t_repeat_yourself) even for 
validation rules, according to the motto "define once, validate everywhere".

Of cause this requires the implementation of generic validators as well as the implementation of JSON _producers_ resp. 
JSON _consumers_ for valid JSON documents in the programming languages that are involved.

E.g. with the Java implementation for this schema a quite complex validation rule that involves conditions that are 
logically linked by AND and OR can be defined like this: 
```java
public class AnyClass {
    static final ValidationRules<Article> articleRules = new ValidationRules<>(Article.class);
    static {
        articleRules.immutable("animalUse",
            ConditionsTopGroup.OR(
                    ConditionsGroup.AND(
                            Condition.of("animalUse", Equals.any(TRUE)),
                            Condition.of("everUsed", Equals.any(TRUE))
                    ),
                    ConditionsGroup.AND(
                            Condition.of("medicalSetId", Equals.notNull())
                    )
            )
        );
    }
}
```
The Java implementation is a _JSON provider_, i.e. it provides a method to serialize the validation rules to JSON.
An application that uses this Java implementation can expose the JSON e.g. via a REST endpoint:

    ValidationRules.serializeToJson(articleRules, otherRules);

With the help of the ES6 implementation a frontend can then check if a object property is immutable and e.g. should be displayed as _disabled_ like this:
```javascript
  article = {
    "animalUse": "true",
    "everUsed": "true",
    "medicalSetId": "S-123-456"
    // other properties omitted
  }
  isImmutable("article", "animalUse", article); // should evaluate to true
```

# Motivation
Validation of input data plays a crucial role in almost any (web) app. Whereby the server-side validation is a must, 
because the server should never trust the client-side. Of course client side validation is also important for a good 
user experience.

The idea for this JSON Schema was inspired by a web application that had _many_ non trivial validation rules. The 
front-end of this application was written in JavaScript (Angular), while the back-end was written in Java. Most of these
validation rules should be utilized also on the client-side. On the one hand to provide good user guidance, on the other
hand to avoid numerous client-server round trips:
- For a _mandatory_ object property the corresponding form input field should be decorated with a visual indicator and 
  the submit button should be disabled as long as the user didn't enter a value for this property.
- For an _immutable_ (a.k.a. _read-only_) object property the corresponding form input field should be set to disabled 
  state.
- For _content related validation rules_ the client should give immediate feedback to the user if the content of the 
  input field doesn't pass the validation check.

## Drawbacks of existing validation frameworks
Although the Java Bean Validation framework has become the de-facto standard for Java based apps, it has some drawbacks 
and limitations:
- Most importantly: the validation rules can't be _reused_ easily at least in non-java components, and have to be 
  re-implemented.
- Because of the annotation approach, real POJOs can not be validated.
- The functionality of some standard annotations are quite limited. E.g. with @Future it is not possible to validate 
  that a date is 2 days in the future.
- Cross property validations requires lots of code.
- Rules that dependent on user permissions cannot be expressed in compact form [TODO check statement].

## Required features of a flexible and expressive framework
It should be possible to define validation rules 
- for hierarchical (a.k.a. _nested_) properties
- that involve conditions for any number of properties (a.k.a. _multi property validation_)
- that allow to combine these conditions mit logical AND resp. OR
- for arrays resp. list properties
  that refer to individual arrays resp. list elements
- that depends on user permissions

# Documentation
The documentation of all kinds of possible validation rules is _based on a close-to-life example_ to make the exemplary 
rules a little more descriptive.

## Close-to-life example
Let's say we work for a company that rents _medical equipment_. Each medical _article_ may contain several 
_accessories_. Articles are grouped in _medical sets_. The equipment is stored in warehouses and delivered to customers 
locations. Staff members schedule _reservations_ on behalf of their customers.

### Example objects
These are example objects for the aforementioned entity types with some of their properties. The more technical 
properties like _id_, _createdBy_ etc. are omitted:

- Accessory
```json
  {
    "name": "Biopsy Forcep",
    "number": "BF-123"
  }
```
- Article
```json
  {
    "name": "Diagnostic Video Colonoscope",
    "number": "DVC-H123T/Z",
    "status": "ACTIVE",
    "animalUse": "true",
    "everUsed": "false",
    "medicalSetId": null,
    "responsibleUser": null,
    "accessories": [
    ]
  }
```
- MedicalSet
```json
  {
    "name": "Endoscope set advanced",
    "number": "ESA-1",
    "status": "COMMISSIONING",
    "animalUse": "true",
    "articles": [
    ]
  }
```
- Reservation with customer
```json
  {
    "status": "PREPARATION",
    "startDate": "2021-02-01",
    "endDate": "20121-02-28",
    "customer": {
      "name": "Estetical Pet Clinic",
      "status": "GOLD",
      "address": {
        "city": "New York City",
        "zipCode": "10001"
      }
    },
    "medicalSets": [
    ]
  }
```

### Example validation rules
A fictitious requirements document could mention validation rules like this:
1. Only managers are allowed to set customer level to PLATINUM.
2. Allowed article status transitions are: NEW -> ACTIVE, NEW -> INACTIVE, ACTIVE -> INACTIVE/DECOMMISSIONED, INACTIVE 
   -> ACTIVE/DECOMMISSIONED".
3. An article must have a responsible user if the article status is not NEW, and the article is not assigned to a 
   MedicalSet.
4. If an article is used for the first time, it has to be flagged as such. This flag must never be reset.
5. The _animalUse_ property of an article must not be changed if a) it is assigned to a medical set, or b) it has been 
   used once for animals.
6. The reservation start date must be 3 days in the future.
7. The reservation end date must be after the start date.
8. A reservation not in status PREPARATION must contain 1 to 3 medical sets. If the customer has status PLATINUM, it may
   contain up to 5 medical sets.

## JSON structure
### Top-level content
A valid JSON contains 5 name/value pairs:
- _schema-version_ specifies the version of the JSON schema in use.
- _mandatoryRules_ is an object that _may contain_ a name/value pair for each _entity type_ that has validation rules 
  regarding mandatory properties.
- _immutableRules_ is an object that _may contain_ a name/value pair for each _entity type_ that has validation rules 
  regarding immutable properties.
- _contentRules_ is an object that _may contain_ a name/value pair for each _entity type_ that has validation rules 
  regarding the content of properties.
- _updateRules_ is an object that _may contain_ a name/value pair for each _entity type_ that has validation rules 
  regarding the updated content of properties, i.e. that describe the allowed transitions between the original and the 
  modified content.

```json
{
  "schema-version": "0.2",
  "mandatoryRules": {},
  "immutableRules": {},
  "contentRules": {},
  "updateRules": {}
}
```

### Entity Type related validation rules
Validation rules regarding the properties of an entity type are defined by a name/value pair where the name is the name 
of the entity type. The value is an object that _may contain_ a name/value pair for each property of that type.
```json
  {
    "article": {},
    "customer": {},
    "reservation": {}
  }
```

### Property related validation rules
Validation rules for a single property of an entity type are defined by a name/value pair.

The name of the pair is determined by the _name of the property_, i.e.
- for a _simple property_ it is simply its name, e.g.
  - > "responsibleUser"
- for a _nested property_ the name is build by concatenating the property names of the access path by using "." (full 
  stop) separators, e.g. 
  - > "customer.address.city"
- for _properties of objects that are part of arrays_, the names of the array properties can be appended by an _array 
  index_ definition _[x]_, where _x_ is either
  - a _single index value_ of the expected array position, e.g.
    - > "medicalSets[0].articles[0].animalUse"
  - or _a comma separated list_ of index values, e.g.
    - > "medicalSets[1,2,3].articles[4,5].animalUse"
  - or _a range definition_ of two index values separated by "-" (minus), e.g.
    - > "medicalSets[1-3].articles[4-5].animalUse"
  - or _a start-step definition_ of two index values separated by "/" (slash), where the first value specifies the first
    array position and the second values defines the step size to the other array positions, e.g.
    - > "medicalSets[2/1].articles[0/2].animalUse"
  - or _a star (\*)_ as a shortcut for the interval defnition [0/1], e.g.
    - > "medicalSets[\*].articles[\*].animalUse"

The value is an **array** that may contain different types of _condition objects_.
- For validation rules regarding **mandatory** and **immutable** properties the array **may be empty**. That means that 
  this property is mandatory resp. immutable, period. Technically speaking the validation rule always evaluates to 
  _true_.
- For **content** and **update** validation rules the array **must not be empty**, because there must be at least one 
  statement about the _expected content_ of the property the rule is defined for.
```json
    {
      "status": [],
      "customer.address.city": [],
      "medicalSets[0].articles[0].animalUse": []
    }
```

### Condition types and objects
Condition objects specify _further conditions_ under which the validation rules will validate to true. There are 
represented by different name/values pairs:
1. The first pair with the name _constraint_ is used for conditions that relate to the _content of the property_ the 
   validation rule is defined for. The value is an [elementary constraint object](#Elementary%20constraints) that 
   defines the allowed content of this property. This type of condition is required for **content** and **update** rules
   and not allowed for **mandatory** and **immutable** ones.
   > JSON for example validation rule: "The article name length must be between 5 and 100 characters":
   ```json
   { 
     "contentRules": {
       "article": {
         "name": [
           {
             "constraint": {
               "type": "SIZE",
               "min": 5,
               "max": 100
             }
           }
         ]
       }
     }
   }
   ```
2. The second pair with the name _permissions_ is used to restrict the validity of the validation rule to certain _user 
   permissions_. The value is an object with 2 names: _type_ with value _ANY_ and _values_ with a list of allowed 
   permission names.
   > JSON for example validation rule: "The article name must not be modified if the user (who wants to insert resp. 
   > update the object) owns any of the permission resp. role APPRENTICE or REVIEWER":
    ```json
    { 
      "immutableRules": {
        "article": {
          "name": [
            {
              "permissions": {
                "type": "ANY",
                "values": [
                  "APPRENTICE",
                  "REVIEWER"
                ]
              }
            }
          ]
        }
      }
    }
    ```

3. Often, the decision whether a validation rule should be applied depends on conditions on other _related properties_. 
   
   If there are more than one of these conditions, they have to be connected either via a logical _AND operation_, a _OR 
   operation_ or even both.
   
   Let _a, b, c_ and _d_ be 4 of these conditions. Then it should be possible to define logical expressions like
   
   ``a AND b AND c AND d``,
   
   ``a OR b OR c OR d``,
   
   ``a AND b OR c AND d``,
   
   ``(a OR b) AND (c OR d)``
   
   and similar variants.
    
   - 1. The third pair has the name _condition_ and is used to define a _single one of these conditions_.
        The value is an object with 2 name/value pairs: the value of the name _property_ is the name of the property 
        (as defined in [Property related validation rules](#Property%20related%20validation%20rules)) this condition
        is defined for.
        The value of the name _constraint_ is an [elementary constraint object](#Elementary%20constraints)
        > JSON for example validation rule: "If an article is used for the first time, it has to be flagged as such. 
        This flag must never be reset"
        ```json
        { 
          "immutableRules": {
            "article": {
              "everUsed": [
                {
                  "condition": {
                    "property": "everUsed",
                    "constraint": {
                      "type": "EQUALS_ANY",
                      "values": [
                        true
                      ]
                    }
                  }
                }
              ]
            }
          }
        }
        ```
   - 2. TODO _multiple of these conditions_ which are logically ANDed resp. ORed.
        ```json
        { 
          "immutableRules": {
            "article": {
              "foo": [
                {
                  "conditionsGroup": {
                    "operator": "AND",
                    "conditions": [
                      {
                        "property": "bar",
                        "constraint": {
                          "type": "SIZE",
                          "min": 1,
                          "max": 3
                        }
                      },
                      {
                        "property": "zoo[*]",
                        "constraint": {
                          "type": "EQUALS_NOT_NULL"
                        }
                      }
                    ]
                  }
                }
              ]
            }
          }
        }
        ```
   - 3. TODO _multiple of these conditions_ which are _both_ logically ANDed and ORed.
       > JSON for example validation rule: "The animalUse property of an article must not be changed if a) it is 
       assigned to a medical set, or b) it has been used once for animals":
        ```json
        { 
          "immutableRules": {
            "article": {
              "foo": [
                {
                  "conditionsTopGroup": {
                    "operator": "OR",
                    "conditionsGroups": [
                      {
                        "operator": "AND",
                        "conditions": [
                          {
                            "property": "medicalSetId",
                            "constraint": {
                              "type": "EQUALS_NOT_NULL"
                            }
                          }
                        ]
                      },
                      {
                        "operator": "AND",
                        "conditions": [
                          {
                            "property": "everUsed",
                            "constraint": {
                              "type": "EQUALS_ANY",
                              "values": [
                                true
                              ]
                            }
                          },
                          {
                            "property": "animalUse",
                            "constraint": {
                              "type": "EQUALS_ANY",
                              "values": [
                                true
                              ]
                            }
                          }
                        ]
                      }
                    ]
                  }
                }
              ]
            }
          }
        }
        ```
    
## Elementary constraints
An elementary contraint can be used as a _content constraint_ or within conditions for _related properties_.

The value of the name _type_ is a string stating the type of the constraint. 
Each type may have type further specific name/values pairs.

### EQUALS constraints
#### EQUALS_ANY
The EQUALS_ANY constraint validates that the mentioned property value equals one of the (static) values listed in the 
array named _values_. It can be applied to string, number, and boolean. If the string complies to the data-time format 
it should be interpreted as such.
```json
    {
      "type": "EQUALS_ANY",
      "values": [
        "ACTIVE",
        "INACTIVE"
      ]
    }
```
#### EQUALS_ANY_REF
With the EQUALS_ANY_REF constraint it is possible to compair the values of different properties. It validates that the 
mentioned property value equals one of the property values referenced by the property names listed in the array named _values_.
```json
    {
      "type": "EQUALS_ANY_REF",
      "values": [
        "articles[0].status",
        "articles[1].status"
      ]
    }
```
#### EQUALS_NONE
TODO
```json
    {
      "type": "EQUALS_NONE",
      "values": [
        "NEW",
        "DECOMMISSIONED"
      ]
    }
```
#### EQUALS_NONE_REF
TODO
```json
    {
      "type": "EQUALS_NONE_REF",
      "values": [
        "articles[0].status",
        "articles[1].status"
      ]
    }
```
#### EQUALS_NULL
TODO
```json
    {
      "type": "EQUALS_NULL"
    }
```
#### EQUALS_NOT_NULL
TODO
```json
    {
      "type": "EQUALS_NOT_NULL"
    }
```
### REGEX_ANY constraint
TODO
```json
    {
      "type": "REGEX_ANY",
      "values": [
        "^[0-9]{5}$"
      ]
    }
```
### SIZE constraint
SIZE validates that the mentioned property value has a size between the attributes min and max. 

It can be applied to
- _string_
- _array_
- _object_
```json
    {
      "type": "SIZE",
      "min": 1,
      "max": 10
    }
```
### RANGE constraint
TODO

### DATE constraints
#### DATE_FUTURE
TODO
#### DATE_PAST
TODO


# Implementations
TODO
- see Cross Language Validation Java (Producer and Validator)
- see Cross Language Validation ES6 (Consumer and Validator)
## Implementation status
TODO

Supported feature | Java  | ES6 |
--- | --- | ---
|JSON producer|+|-|
|JSON consumer|-|+|
|Validator for _mandatory_/rules|+|+|
|Validator for _immutable_ rules|+|+|
|Validator for _content_ rules|+|-|
|Validator for _update_ rules|+|-|
|Validator for _update_ rules|+|-|
|Supports simple property names (e.g. ``responsibleUser``)|+|+|
|Supports nested property names (e.g. ``customer.address.city``)|+|+|
|Supports single-indexed property names (e.g. ``medicalSets[0].articles[0].animalUse``)|+|+|
|Supports multi-indexed property names (e.g. ``medicalSets[1-3].articles[*].animalUse``)|+|-|
|...|?|?|

# TODOs
Handle TODOs ...

Think about possible extensions, e.g.
-  Allow array index definitions like [1-3]#sum, [2$] (last 2), etc.
- ...







