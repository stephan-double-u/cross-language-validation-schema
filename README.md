# Cross Language Validation (CLV) Schema - V0.2
JSON schema that specifies validation rules in a language independant manner to enable cross language validation.

An online, interactive JSON Schema validator can be found here: https://www.jsonschemavalidator.net/

## Table of Contents
* [TL;DR](#tldr)
* [Motivation](#motivation)
* [Documentation](#documentation)
  * [Close-to-life example](#close-to-life-example)
  * [JSON structure](#json-structure)
  * [Elementary constraints](#elementary-constraints)
* [Implementations](#implementations)
* [Thoughts about possible extensions](#thoughts-about-possible-extensions)

# TL;DR
The purpose of this _JSON Schema_ is to describe _complex validation rules_, independent of a specific 
programming language. The resulting JSON documents are intended to be used in applications that involve multiple 
components possibly written in different programming languages where the rules have to be validated in several 
components, e.g. in a frontend written in ES6 and a backend written in Java. Nevertheless, an implementation for that
schema can also be used usefully on its own e.g. in a Java backend.

One objective is to apply the [DRY principle](https://en.wikipedia.org/wiki/Don%27t_repeat_yourself) even for 
validation rules, according to the motto "define once, validate everywhere".

Of course this requires the implementation of generic validators as well as the implementation of JSON _producers_ resp. 
JSON _consumers_ for valid JSON documents in the programming languages that are involved.

E.g. with the [CLV Java implementation](https://github.com/stephan-double-u/cross-language-validation-java) 
for this 
schema a quite complex validation rule that involves conditions that are logically linked by AND and OR can be defined 
like this: 
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
This Java implementation is a _JSON provider_, i.e. it provides a method to serialize the validation rules to JSON.
An application that uses this Java implementation can expose the JSON e.g. via a REST endpoint:

    ValidationRules.serializeToJson(articleRules, otherRules);

With the help of the [CLV ECMAScript 6 implementation](https://github.com/stephan-double-u/cross-language-validation-es6) 
a frontend can then check if a object property is immutable and e.g. should be displayed as _disabled_ like this:
```javascript
  article = {
    "animalUse": "true",
    "everUsed": "true",
    "medicalSetId": "S-123-456"
    // other properties omitted
  }
  animalUseCheckbox.disabled = isPropertyImmutable("article", "animalUse", article);
```

# Motivation
Validation of input data plays a crucial role in almost any (web) app. Whereby the server-side validation is a must, 
because the server should never trust the client-side. Of course client side validation is also important for a good 
user experience.

The idea for this JSON Schema was inspired by a web application that had _many_ non-trivial validation rules. The 
front-end of this application was written in JavaScript (Angular), while the back-end was written in Java. Most of these
validation rules should be utilized also on the client-side. On the one hand to provide good user guidance, on the other
hand to avoid numerous client-server round trips:
- For a _mandatory_ object property the corresponding form input field should be decorated with a visual indicator, and 
  the submit button should be disabled as long as the user didn't enter a value for this property.
- For an _immutable_ (a.k.a. _read-only_) object property the corresponding form input field should be disabled.
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
- Rules that dependent on user permissions cannot be expressed in a compact form.

## Required features of a flexible and expressive validation framework
It should be possible to define validation rules 
- for hierarchical (a.k.a. _nested_) properties
- that involve conditions for any number of properties (a.k.a. _multi property validation_)
- that allow to combine these conditions with logical AND resp. OR
- for arrays resp. list properties it should be possible to refer to individual array resp. list elements
- that may depend on individual user permissions

# Documentation
The documentation of all kinds of possible validation rules is _based on a close-to-life example_ to make the exemplary 
rules more descriptive.

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
2. Allowed article status transitions are: NEW -> ACTIVE, NEW -> INACTIVE, ACTIVE -> INACTIVE / DECOMMISSIONED, INACTIVE 
   -> ACTIVE / DECOMMISSIONED".
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
A valid JSON contains _5 key/value_ pairs:
- _schema-version_ specifies the version of the JSON schema in use.
- _mandatoryRules_ is an object that contains a key/value pair for each _entity type_ that has validation rules 
  regarding mandatory properties.
- _immutableRules_ is an object that contains a key/value pair for each _entity type_ that has validation rules 
  regarding immutable properties.
- _contentRules_ is an object that contains a key/value pair for each _entity type_ that has validation rules 
  regarding the content of properties.
- _updateRules_ is an object that contains a key/value pair for each _entity type_ that has validation rules 
  regarding the _updated_ content of properties, i.e. describe the allowed transitions between the original and the 
  changed content.

Thus, the most minimal valid JSON file (i.e. a file that does not contain any validation rule at all) look like this:
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
Validation rules regarding the properties of an entity type are defined by a key/value pair where the key is the name 
of the entity type. The value is an object that _may contain_ a key/value pair for any property of that type.

A possible value of one of the above-mentioned `*Rules` keys:
```json
  {
    "article": {},
    "customer": {},
    "reservation": {}
  }
```

### Property related validation rules
Validation rules for a single property of an entity type are defined by a key/value pair.

The key of the pair is determined by the _name of the property_, i.e.
- for a _simple property_ it is simply its name, e.g.
  - > "responsibleUser"
- for a _nested property_ the key is build by concatenating the property names of the access path by using "." (full 
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
  - or _a start-step definition_ of two values separated by "/" (slash), where the first value specifies the first
    array position and the second values defines the step size to the other array positions, e.g.
    - > "medicalSets[2/1].articles[0/2].animalUse"
  - or _a star (\*)_ as a shortcut for the interval definition [0/1], e.g.
    - > "medicalSets[\*].articles[\*].animalUse"

All index values are _zero-based_.

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
Condition objects specify _further conditions_ under which the validation rules will be validated. They are 
represented by different key/value pairs:
1. The first key/value pair with the key _constraint_ is used for conditions that relate to the _content of the 
   property_ the validation rule is defined for. The value is an [elementary constraint object](#Elementary-constraints)
   that defines the allowed content of this property. This type of condition _is required for **content** and **update**
   rules_ and _not allowed for **mandatory** and **immutable** ones_.
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
2. The second key/value pair with the key _permissions_ is used to restrict the validity of the validation rule to certain _user 
   permissions_. The value is an object with 2 keys: _type_ with value _ANY_ and _values_ with a list of allowed 
   permission names. This pair is optional.
   > JSON for example validation rule: "The article name must not be modified if the user (who wants to 
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

3. Often the decision whether to apply a validation rule depends on the state of _other properties_. The expectations 
   about the condition of these properties are described in a third key/value pair.
   
   If there are more than one of these conditions, they have to be connected either via a logical _AND operation_, a _OR 
   operation_ or even both.
   
   Let _a, b, c_ and _d_ be 4 of these conditions. Then it should be possible to define logical expressions like
   
   ``a AND b AND c AND d``,
   
   ``a OR b OR c OR d``,
   
   ``a AND b OR c AND d``,
   
   ``(a OR b) AND (c OR d)``
   
   and similar variants.

   Depending on the _number of these conditions_ and _how they are logically connected_, there are _three variants_ of 
   this third key/value pair:
 
   - 1. If a single of these conditions exists, the key of the third pair is _condition_, where the value is an object with 2 
        key/value pairs: the key of one pair is _property_, its value is the name of the property this condition is 
        defined for (as defined in [Property related validation rules](#Property-related-validation-rules)).
        The key of the other pair is _constraint_, its value is an [elementary constraint object](#Elementary-constraints)
        > JSON for example validation rule: "If an article is used for the first time, it has to be flagged as such. 
        This flag must never be reset":
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
   - 2. If there are multiple of these conditions, and they are _all linked with either AND or OR_, the key of the 
        third pair is _conditionsGroup_, where the value is an object with 2 key/value pairs: the key of one pair is 
        _operator_, its value is either _AND_ or _OR_. The key of the other pair is _constraints_, its value is an
        array of [elementary constraint objects](#Elementary-constraints).
        > JSON for example validation rule: "The animalUse property of an article must not be changed if it has been 
        used once for animals":
        ```json
        { 
          "immutableRules": {
            "article": {
              "animalUse": [
                {
                  "conditionsGroup": {
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
                }
              ]
            }
          }
        }
        ```
   - 3. If there are multiple of these conditions, and if the logical relation between the conditions is complex so 
        that they are _linked with both AND and OR_, the key of the third pair is _conditionsTopGroup_, where the value
        is an object with 2 key/value pairs: the key of one pair is _operator_, its value is either _AND_ or _OR_. 
        The key of the other pair is _conditionsGroups_, its value is an array of _ConditionsGroup objects_ as 
        described above.
        > JSON for example validation rule: "The animalUse property of an article must not be changed if (a) it is 
        assigned to a medical set, or (b) it has been used once for animals":
        ```json
        { 
          "immutableRules": {
            "article": {
              "animalUse": [
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
An elementary constraint is used as a _content constraint_ or within conditions for "related properties", i.e. it 
is used as the _value of any constraint key_.
It is an object consisting of one or more key/value pairs. 

The key of the first pair is _type_, its value is a string stating the type of the constraint. Each type can have 
further type-specific key/value pairs.

### EQUALS constraints
#### EQUALS_ANY
The EQUALS_ANY constraint checks whether the value of the associated property matches any of the values listed in the
array named _values_. 
```json
    {
      "type": "EQUALS_ANY",
      "values": [
        "ACTIVE",
        "INACTIVE"
      ]
    }
```
This constraint can be applied to properties of type
- _string_,
- _number_
- _boolean_

If the string complies to the _date_ resp. _date-time_ format (according to 
[RFC 3339, section 5.6](https://datatracker.ietf.org/doc/html/rfc3339#section-5.6)) it should be interpreted as such.

#### EQUALS_ANY_REF
With the EQUALS_ANY_REF constraint it is possible to compare the values of properties. It validates that the
value of the associated property equals any of the property values referenced by the property names listed in the array 
named _values_.
```json
    {
      "type": "EQUALS_ANY_REF",
      "values": [
        "articles[0].status",
        "articles[1].status"
      ]
    }
```
This constraint can be applied to properties of type
- _string_,
- _number_
- _boolean_

If the string complies to the _date_ resp. _date-time_ format (according to
[RFC 3339, section 5.6](https://datatracker.ietf.org/doc/html/rfc3339#section-5.6)) it should be interpreted as such.

The type of the associated property must equal the type of the properties referenced by the property names listed in the
array named _values_.

#### EQUALS_NONE
The EQUALS_NONE constraint checks whether the value of the associated property does _not match_ any of the values listed
in the array named _values_.
```json
    {
      "type": "EQUALS_NONE",
      "values": [
        "NEW",
        "DECOMMISSIONED"
      ]
    }
```
This constraint can be applied to properties of type
- _string_
- _number_
- _boolean_

If the string complies to the _data_ resp. _data-time_ format (according to
[RFC 3339, section 5.6](https://datatracker.ietf.org/doc/html/rfc3339#section-5.6)) it should be interpreted as such.

#### EQUALS_NONE_REF
With the EQUALS_NONE_REF constraint it is possible to compare the _values of properties_. It validates that the
value of the associated property does _not match_ any of the property values referenced by the property names listed in 
the array named _values_.
```json
    {
      "type": "EQUALS_NONE_REF",
      "values": [
        "articles[0].status",
        "articles[1].status"
      ]
    }
```
This constraint can be applied to properties of type
- _string_
- _number_
- _boolean_

If the string complies to the _date_ resp. _date-time_ format (according to
[RFC 3339, section 5.6](https://datatracker.ietf.org/doc/html/rfc3339#section-5.6)) it should be interpreted as such.

The type of the associated property must equal the type of the properties referenced by the property names listed in the
array named _values_.

#### EQUALS_NULL
The EQUALS_NULL constraint checks whether the value of the associated property is _null_.
```json
    {
      "type": "EQUALS_NULL"
    }
```
This constraint can be applied to properties of type
- _string_
- _number_
- _boolean_
- _array_
- _object_

#### EQUALS_NOT_NULL
The EQUALS_NOT_NULL constraint checks whether the value of the associated property is _not null_.
```json
    {
      "type": "EQUALS_NOT_NULL"
    }
```
This constraint can be applied to properties of type
- _string_
- _number_
- _boolean_
- _array_
- _object_

### REGEX_ANY constraint
The REGEX_ANY constraint checks whether the value of the associated property does _match_ any of the _regular 
expressions_ listed in the array named _values_.
```json
    {
      "type": "REGEX_ANY",
      "values": [
        "^[0-9]{5}$"
      ]
    }
```
This constraint can be applied to properties of type
- _string_
- _number_

**NOTE**: The schema does not limit the regex features that could be used. The permissible range of regex features 
usually results from the lowest common denominator of the languages involved. 
For instance, a `REGEX_ANY` validation rule that should be 'shared' between a Java backend and a ES6 frontend, should 
not use _inline modifiers_ (e.g. `(?i)`),
[because ES6 has no support for it](https://en.wikipedia.org/wiki/Comparison_of_regular_expression_engines#Part_2).
(At least as long as the ES6 implementation does not use externals libraries to augment their build-in regex 
capabilities, like e.g. [XRegExp](https://xregexp.com/))

### SIZE constraint
The SIZE constraint validates that the size (resp. length) of the associated property value is between the number values of the keys
_min_ resp. _max_. 
```json
    {
      "type": "SIZE",
      "min": 0,
      "max": 10
    }
```

This constraint can be applied to properties of type
- _string_ - the size of a string corresponds to the number string characters.
- _array_ - the size of an array corresponds to the number of array elements.
- _object_ - the size of an object corresponds to the number of object keys.

At least one of the keys _min_ or _max_ must be specified. The other key is optional.

If both keys are specified, the _min-value_ must not be greater than the _max-value_.

The values of the keys _min_ and _max_ must be > 0 (zero).

### RANGE constraint
The RANGE constraint checks whether the value of the associated property is within the range defined by the numeric 
values of the keys _min_ and _max_.
```json
    {
      "type": "RANGE",
      "min": 0,
      "max": 10
    }
```
This constraint can be applied to properties of type
- **TODO**

At least one of the keys _min_ or _max_ must be specified. The other key is optional.

If both keys are specified, the _min-value_ must not be greater than the _max-value_.


### DATE constraints
#### DATE_FUTURE
The DATE_FUTURE constraint checks whether the value of the associated property is a date that is 0 or more days _in the 
future_. The number of days is defined as value of the key _days_.
```json
    {
      "type": "DATE_FUTURE",
      "days": 0
    }
```
This constraint can be applied to properties of type
- **TODO**

#### DATE_PAST
The DATE_PAST constraint checks whether the value of the associated property is a date that is 0 or more days _in the
past_. The number of days is defined as value of the key _days_.
```json
    {
      "type": "DATE_PAST",
      "days": 0
    }
```
This constraint can be applied to properties of type
- **TODO**


# Implementations
- [Cross Language Validation Java](https://github.com/stephan-double-u/cross-language-validation-java) implements a 
  Validator and a Producer for this schema in Java.
-[Cross Language Validation ECMAScript 6](https://github.com/stephan-double-u/cross-language-validation-es6) implements
  a Validator and a Consumer for this schema in ECMAScript 6.

## Implementation status
TODO

|Supported feature | Java | ES6 |
|--- |---|---|
|JSON Producer| + | -   |
|JSON Consumer| - | +   |
|Validator for _mandatory_ rules| + | +   |
|Validator for _immutable_ rules| + | +   |
|Validator for _content_ rules| + | -   |
|Validator for _update_ rules| + | -   |
|Supports simple property names (e.g. ``responsibleUser``)| + | +   |
|Supports nested property names (e.g. ``customer.address.city``)| + | +   |
|Supports single-indexed property names (e.g. ``medicalSets[0].articles[0].animalUse``)| + | +   |
|Supports multi-indexed property names (e.g. ``medicalSets[1-3].articles[*].animalUse``)| + | -   |
|...| ? | ?   |

# Thoughts about possible extensions
- DATE_FUTURE/PAST with _minDays_ and _maxDays_?
- DATE_WEEKDAY_ANY with _values_ ["MONDAY", ...]}?
- Array index definitions with 'functions'?
  - e.g. foo[*]#sum, foo[*].bar[*].name#unique
  - Remark: min/max not needed, because e.g. the content constraint
    
    "article[*].price#max -> Range.max(1000)" is equivalent to
    
    "article[*].price -> Range.max(1000)"
    
- Array index definition 'last N elements' (e.g. [2L])?






