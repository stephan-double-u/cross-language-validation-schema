# Cross Language Validation (CLV) Schema - V0.7
JSON schema that specifies validation rules in a language independent manner to enable cross language validation.<br>
An online, interactive JSON Schema validator for this schema can be found here:
https://www.jsonschemavalidator.net/s/ByoXo237

## Table of Contents
- [TL;DR](#tldr)
- [Motivation](#motivation)
  - [Drawbacks of existing validation frameworks](#drawbacks-of-existing-validation-frameworks)
  - [Required features of a flexible and expressive validation framework](#required-features-of-a-flexible-and-expressive-validation-framework)
- [Close-to-life example](#close-to-life-example)
  - [Example objects](#example-objects)
  - [Example validation rules](#example-validation-rules)
- [Documentation JSON structure](#documentation-json-structure)
  - [Top-level content](#top-level-content)
  - [Entity Type related validation rules](#entity-type-related-validation-rules)
  - [Property related validation rules](#property-related-validation-rules)
  - [Condition types and objects](#condition-types-and-objects)
    - [Content constraint](#content-constraint)
    - [Permissions constraint](#permissions-constraint)
    - [Constraints about referenced properties](#constraints-about-referenced-properties)
    - [Error code control](#error-code-control)
  - [Elementary constraints](#elementary-constraints)
    - [EQUALS\_ANY](#equals_any)
    - [EQUALS_ANY_REF](#equalsanyref)
    - [EQUALS_NONE](#equalsnone)
    - [EQUALS_NONE_REF](#equalsnoneref)
    - [EQUALS_NULL](#equalsnull)
    - [EQUALS_NOT_NULL](#equalsnotnull)
    - [REGEX_ANY](#regexany)
    - [SIZE](#size)
    - [RANGE](#range)
    - [FUTURE_DAYS](#futuredays)
    - [PAST_DAYS](#pastdays)
    - [PERIOD_DAYS](#perioddays)
    - [WEEKDAY_ANY](#weekdayany)
- [Requirements for the implementers](#requirements-for-the-implementers)
  - [Producer](#producer)
  - [Consumer](#consumer)
  - [Validator](#validator)
    - [Rule validation sequence](#rule-validation-sequence)
    - [Validation error codes](#validation-error-codes)
- [Implementations](#implementations)
  - [Implementation status](#implementation-status)
- [Thoughts about possible extensions](#thoughts-about-possible-extensions)

# TL;DR
The purpose of this _JSON Schema_ is to describe _complex validation rules_, independent of a specific 
programming language. The resulting JSON documents are intended to be used in applications that involve multiple 
components possibly written in different programming languages where the rules have to be validated in several 
components. E.g. in a frontend written in ES6 and a backend written in Java.

One objective is to apply the [DRY principle](https://en.wikipedia.org/wiki/Don%27t_repeat_yourself) even for 
validation rules, according to the motto "define once, validate everywhere".

Nevertheless, an implementation for that schema can also be used usefully on its own e.g. in a Java backend.

Of course this requires the implementation of generic validators as well as the implementation of JSON _producers_ 
resp. JSON _consumers_ for valid JSON documents in the programming languages that are involved.

As an example of what is possible, we assume we have this quite complex validation rule that involves conditions that 
are logically linked by AND and OR:

> The _animalUse_ property of an article must not be changed if<br>
> a) it is assigned to a medical set<br>
> OR<br>
> b) it has been used once for animals - i.e. the _everLeftWarehouse_ AND _animalUse_ properties are both _true_.

With the [CLV Java implementation](https://github.com/stephan-double-u/cross-language-validation-java) 
for this schema this validation rule can be defined like this: 
```java
public class AnyClass {
    static final ValidationRules<Article> ARTICLE_RULES = new ValidationRules<>(Article.class);
    static {
        articleRules.immutable("animalUse",
            ConditionsTopGroup.OR(
                    ConditionsGroup.AND(
                            Condition.of("medicalSetId", Equals.notNull())),
                    ConditionsGroup.AND(
                            Condition.of("animalUse", Equals.any(TRUE)),
                            Condition.of("everLeftWarehouse", Equals.any(TRUE)))));
    }
}
```
This Java implementation is a _JSON provider_, i.e. it provides a method to serialize the validation rules to JSON.
An application that uses this Java implementation can expose the JSON e.g. via a REST endpoint:

    ValidationRules.serializeToJson(ARTICLE_RULES);

With the help of the 
[CLV ECMAScript 6 implementation](https://github.com/stephan-double-u/cross-language-validation-es6) 
a frontend can then check if an object property is immutable and e.g. should be displayed as _disabled_ like this:
```javascript
// retrieved e.g. via GET /articles/12345
article = {
  "id": 12345,
  "animalUse": "true",
  "everLeftWarehouse": "true",
  "medicalSetId": "S-123-456"
  // other properties omitted
}
animalUseCheckbox.disabled = isPropertyImmutable("article", "animalUse", article);
```

&rarr; See the [CLV Demo App](https://github.com/stephan-double-u/cross-language-validation-demo) as a comprehensive 
example of how to use the framework.

# Motivation
Validation of input data plays a crucial role in almost any (web) app. Whereby the server-side validation is a must, 
because the server should never trust the client-side. Of course client side validation is also important for a good 
user experience.

The idea for this JSON Schema was inspired by a web application that had _many_ non-trivial validation rules that 
additionally depended on user permissions.
The front-end of this application was written in JavaScript (Angular), while the back-end was written in Java. 
Most of these validation rules should be utilized also on the client-side.
On the one hand to provide good user guidance, on the other hand to avoid numerous client-server round trips:
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
- The functionality of some standard annotations are quite limited. E.g. with `@Future` it is not possible to validate 
  that a date is 2 days in the future.
- Cross property validations cannot be expressed in a compact forme.
- Rules that dependent on user permissions cannot be expressed in a compact form.
- And a perhaps more philosophical aspect: Because of the annotation approach, real POJOs can not be (easily) validated.

## Required features of a flexible and expressive validation framework
It should be possible to define validation rules 
- for hierarchical (a.k.a. _nested_) properties
- that involve conditions for any number of properties (a.k.a. _multi property validation_)
- that allow to combine these conditions with logical AND resp. OR
- for arrays resp. list properties it should be possible to refer to individual array resp. list elements
- that may depend on individual user permissions

# Close-to-life example
The documentation of all kinds of possible validation rules is _based on this close-to-life example_ to make the 
exemplary rules more descriptive.

Let's say we work for a company that rents _medical equipment_. Each medical _article_ may contain several 
_accessories_. Articles are grouped in _medical sets_. The equipment is stored in warehouses and delivered to customers 
locations, e.g. hospital or animal clinics. Staff members schedule _reservations_ on behalf of their customers.

## Example objects
These are example objects for the aforementioned entity types with some of their properties. The more technical 
properties like _id_, _createdBy_ etc. are omitted:

- Accessory
```json
  {
    "name": "Biopsy Forcep",
    "number": "BF-123"
  }
```
- AccessoryAmount
```json
  {
    "accessoryNumber": "BF-123",
    "amount": "3"
  }
```
- Article
```json
  {
    "name": "Diagnostic Video Colonoscope",
    "number": "DVC-H123T/Z",
    "status": "ACTIVE",
    "animalUse": "true",
    "everLeftWarehouse": "false",
    "medicalSetId": null,
    "responsibleUser": null,
    "accessoriesAmount": [
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

## Example validation rules
A fictitious requirements document could mention validation rules like this:
1. If an article is delivered for the first time, it has to be flagged as such (property _everLeftWarehouse_).
   This flag must never be reset.
2. The _animalUse_ property of an article must not be changed if
   a) it is assigned to a medical set, or
   b) it has been used once for animals (i.e. the _everLeftWarehouse_ and _animalUse_ flags have been set).
3. Only managers are allowed to set customer level to PLATINUM.
4. Allowed article status transitions are: NEW -> ACTIVE, NEW -> INACTIVE, ACTIVE -> INACTIVE / DECOMMISSIONED, INACTIVE 
   -> ACTIVE / DECOMMISSIONED".
5. An article must have a responsible user if the article status is not NEW, and the article is not assigned to a 
   MedicalSet.
6. The reservation start date must be 3 days in the future.
7. The reservation end date must be after the start date.
8. A reservation not in status PREPARATION must contain 1 to 3 medical sets. If the customer has status PLATINUM, it may
   contain up to 5 medical sets.

# Documentation JSON structure
## Top-level content
A valid JSON contains _5 key-value_ pairs:
- _schemaVersion_ specifies the version of the JSON schema in use.
- _mandatoryRules_ is an object that contains a key-value pair for each _entity type_ that has validation rules 
  regarding mandatory properties.
- _immutableRules_ is an object that contains a key-value pair for each _entity type_ that has validation rules 
  regarding immutable properties.
- _contentRules_ is an object that contains a key-value pair for each _entity type_ that has validation rules 
  regarding the content of properties.
- _updateRules_ is an object that contains a key-value pair for each _entity type_ that has validation rules 
  regarding the _updated_ content of properties, i.e. describe the allowed transitions between the original and the 
  changed content.

Thus, the most minimal valid JSON file (i.e. a file that does not contain any validation rule at all) look like this:
```json
{
  "schemaVersion": "0.7",
  "mandatoryRules": {},
  "immutableRules": {},
  "contentRules": {},
  "updateRules": {}
}
```

## Entity Type related validation rules
Validation rules regarding the properties of an entity type are defined by a key-value pair where the key is the name 
of the entity type. The value is an object that _may contain_ a key-value pair for any property of that type.

A possible value for one of the above-mentioned `Rules` keys:
```json
  {
    "article": {},
    "customer": {},
    "reservation": {}
  }
```

## Property related validation rules
Validation rules for a single property of an entity type are defined by a key-value pair.

**The key of the pair** is determined by the _name of the property_, i.e.
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
  - TODO: describe terminal aggregate functions #sum and #distinct
    - > "medicalSets[\*].articles[\*].accessoriesAmount[\*].amount#sum"
    - > "medicalSets[\*].articles[\*].accessoriesAmount[\*].accessoryNumber#distinct"
 
All index values are _zero-based_.

**The value of the pair** is an _array_ that may contain different types of _condition objects_.
- For validation rules regarding **mandatory** and **immutable** properties the array **may be empty**. That means that 
  this property is mandatory resp. immutable, period. Technically speaking the validation rule always evaluates to 
  _true_.
- For **content** and **update** validation rules the array **must not be empty**, because there must be at least one 
  statement about the _expected content_ of the property the rule is defined for.

Possible mandatory rules for the entity type `reservation`:
```json
    {
      "status": [],
      "customer.address.city": [],
      "medicalSets[*].articles[*].animalUse": []
    }
```

## Condition types and objects
Condition objects specify _further conditions_ under which the validation rules will be validated. They are 
represented by different key-value pairs.

### Content constraint
The first key-value pair with the key _constraint_ is used for conditions that relate to the _content of the 
property itself_ for which the validation rule is defined. The value is an 
[elementary constraint object](#Elementary-constraints) that defines the allowed content of this property. 

This type of condition _is required for **content** and **update** rules_ and _not allowed for 
**mandatory** and **immutable** ones_.
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

### Permissions constraint
The second key-value pair with the key _permissions_ is used to **restrict the validity of the validation rule** to 
certain _user permissions_. The value is an object with 2 keys: _type_ with value _ALL_, _ANY_ or _NONE_ and 
_values_ with a list of allowed permission names.

This pair is optional.

A missing permissions constraint means, that the validation rule should always be validated, regardless of any 
permissions the user might have.

A permissions constraint of type _ALL_ means, that the validation rule should only be validated, if the user has
(at least) _all_ the permissions listed in the _values_ array.

A permissions constraint of type _ANY_ means, that the validation rule should only be validated, if the user has
at least _one_ of the permissions listed in the _values_ array.

A permissions constraint of type _NONE_ means, that the validation rule should only be validated, if the user 
does not have any permission from the _values_ array.

> JSON for example validation rule: "The article name must not be modified if the user (who wants to 
> update the object) owns **any of** the permission resp. role APPRENTICE or REVIEWER":
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

### Constraints about referenced properties
Often the decision whether to apply a validation rule depends on the state of _other properties_.
Or even on the state of the same properties, in case of changing the value of the property during an update.
The expectations about the condition of these properties are described in a third key-value pair.

This pair _is required for **update** rules_.

If there are more than one of these conditions, they have to be connected either via a logical _AND operation_, a _OR 
operation_ or even both.

Let _a, b, c_ and _d_ be 4 of these conditions. Then it should be possible to define logical expressions like<br>
``a AND b AND c AND d``,<br>
``a OR b OR c OR d``,<br>
``a AND b OR c AND d``,<br>
``(a OR b) AND (c OR d)``<br>
and similar variants.

Depending on the _number of these conditions_ and _how they are logically connected_, there are _three variants_ of 
this third key-value pair:

1. If a single of these conditions exists, the key of the third pair is _condition_, where the value is an object with 2 
  key-value pairs: the key of one pair is _property_, its value is the name of the property this condition is 
  defined for (as defined in [Property related validation rules](#Property-related-validation-rules)).
  The key of the other pair is _constraint_, its value is an [elementary constraint object](#Elementary-constraints)
    > JSON for example validation rule: "If an article is used for the first time, it has to be flagged as such. 
    This flag must never be reset":
    ```json
    { 
      "immutableRules": {
        "article": {
          "everLeftWarehouse": [
            {
              "condition": {
                "property": "everLeftWarehouse",
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
2. If there are multiple of these conditions, and they are _all linked with either AND or OR_, the key of the 
  third pair is _conditionsGroup_, where the value is an object with 2 key-value pairs: the key of one pair is 
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
                    "property": "everLeftWarehouse",
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
3. If there are multiple of these conditions, and if the logical relation between the conditions is complex so 
  that they are _linked with both AND and OR_, the key of the third pair is _conditionsTopGroup_, where the value
  is an object with 2 key-value pairs: the key of one pair is _operator_, its value is either _AND_ or _OR_. 
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
                        "property": "everLeftWarehouse",
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

### Error code control
TODO describe in more detail
If a validation rule is not met, an error code is generated as defined in chapter.
[Validation error codes](#validation-error-codes)
```json
{
          "errorCodeControl": {
            "useType": "AS_SUFFIX",
            "code": "#suffix"
          }
}
```

## Elementary constraints
An elementary constraint is used as a _content constraint_ or within conditions for "related properties", i.e. it 
is used as the _value of any constraint key_.
It is an object consisting of one or more key-value pairs.<br>
The key of the first pair is always _type_, its value is a string stating the type of the constraint. Each type can have 
further type-specific key-value pairs.

### EQUALS\_ANY
The EQUALS_ANY constraint checks whether the value of the associated property matches any of the values listed in the
array named _values_.<br>
Example:
```json
    {
      "type": "EQUALS_ANY",
      "values": [
        "ACTIVE",
        "INACTIVE"
      ],
      "nullEqualsTo": true
    }
```
This constraint can be applied to properties of type:
- _string_
- _number_
- _boolean_

Requirements:
- If the string complies to the _date_ (e.g. ```"2022-12-31"```) resp. _date-time_ (e.g. ```"2022-12-31T23:59:59Z"```) 
format (according to [RFC 3339, section 5.6](https://datatracker.ietf.org/doc/html/rfc3339#section-5.6)) it should be 
interpreted as such.
- The array must contain at least one value.
- _Null_ values are not allowed.
- The optional key _nullEqualsTo_ determines how this constraint should be evaluated if the value of
the associated property is _null_.
- For this constraint the _nullEqualsTo_ default value is _false_.

### EQUALS_ANY_REF
With the EQUALS_ANY_REF constraint it is possible to compare the values of properties. It validates that the
value of the associated property equals any of the property values referenced by the property names listed in the array 
named _values_.<br>
Example:
```json
    {
      "type": "EQUALS_ANY_REF",
      "values": [
        "articles[0].status",
        "articles[1].status"
      ],
      "nullEqualsTo": true
    }
```
This constraint can be applied to properties of type:
- _string_
- _number_
- _boolean_

Requirements:
- The type of the associated property must equal the type of the properties referenced by the property names listed in the
  array named _values_.
- If the string complies to the _date_ (e.g. ```"2022-12-31"```) resp. _date-time_ (e.g. ```"2022-12-31T23:59:59Z"```)
  format (according to [RFC 3339, section 5.6](https://datatracker.ietf.org/doc/html/rfc3339#section-5.6)) it should be
  interpreted as such.
- The array must contain at least one value.
- _Null_ values are not allowed.
- The optional key _nullEqualsTo_ determines how this constraint should be evaluated if the value of
  the associated property is _null_.
- For this constraint the _nullEqualsTo_ default value is _false_.

### EQUALS_NONE
The EQUALS_NONE constraint checks whether the value of the associated property does _not match_ any of the values listed
in the array named _values_.<br>
Example:
```json
    {
      "type": "EQUALS_NONE",
      "values": [
        "NEW",
        "DECOMMISSIONED"
      ],
      "nullEqualsTo": false
    }
```
This constraint can be applied to properties of type:
- _string_
- _number_
- _boolean_

Requirements:
- The type of the associated property must equal the type of the properties referenced by the property names listed in the
  array named _values_.
- If the string complies to the _date_ (e.g. ```"2022-12-31"```) resp. _date-time_ (e.g. ```"2022-12-31T23:59:59Z"```)
  format (according to [RFC 3339, section 5.6](https://datatracker.ietf.org/doc/html/rfc3339#section-5.6)) it should be
  interpreted as such.
- The array must contain at least one value.
- _Null_ values are not allowed.
- The optional key _nullEqualsTo_ determines how this constraint should be evaluated if the value of
  the associated property is _null_.
- For this constraint the _nullEqualsTo_ default value is _true_.

### EQUALS_NONE_REF
With the EQUALS_NONE_REF constraint it is possible to compare the _values of properties_. It validates that the
value of the associated property does _not match_ any of the property values referenced by the property names listed in 
the array named _values_.<br>
Example:
```json
    {
      "type": "EQUALS_NONE_REF",
      "values": [
        "articles[0].status",
        "articles[1].status"
      ],
      "nullEqualsTo": false
    }
```
This constraint can be applied to properties of type:
- _string_
- _number_
- _boolean_

Requirements:
- The type of the associated property must equal the type of the properties referenced by the property names listed in the
  array named _values_.
- If the string complies to the _date_ (e.g. ```"2022-12-31"```) resp. _date-time_ (e.g. ```"2022-12-31T23:59:59Z"```)
  format (according to [RFC 3339, section 5.6](https://datatracker.ietf.org/doc/html/rfc3339#section-5.6)) it should be
  interpreted as such.
- The array must contain at least one value.
- _Null_ values are not allowed.
- The optional key _nullEqualsTo_ determines how this constraint should be evaluated if the value of
  the associated property is _null_.
- For this constraint the _nullEqualsTo_ default value is _true_.

### EQUALS_NULL
The EQUALS_NULL constraint checks whether the value of the associated property is _null_.
```json
    {
      "type": "EQUALS_NULL"
    }
```
This constraint can be applied to properties of type:
- _string_
- _number_
- _boolean_
- _array_
- _object_

### EQUALS_NOT_NULL
The EQUALS_NOT_NULL constraint checks whether the value of the associated property is _not null_.
```json
    {
      "type": "EQUALS_NOT_NULL"
    }
```
This constraint can be applied to properties of type:
- _string_
- _number_
- _boolean_
- _array_
- _object_

### REGEX_ANY
The REGEX_ANY constraint checks whether the value of the associated property does _match_ any of the _regular 
expressions_ listed in the array named _values_.<br>
Example:
```json
    {
      "type": "REGEX_ANY",
      "values": [
        "^[0-9]{5}$"
      ]
    }
```
This constraint can be applied to properties of type:
- _string_
- _number_

**NOTE**: The schema does not limit the regex features that could be used. The permissible range of regex features 
usually results from the lowest common denominator of the languages involved. 
For instance, a `REGEX_ANY` validation rule that should be 'shared' between a Java backend and a ES6 frontend, should 
not use _inline modifiers_ (e.g. `(?i)`),
[because ES6 has no support for it](https://en.wikipedia.org/wiki/Comparison_of_regular_expression_engines#Part_2).
(At least as long as the ES6 implementation does not use externals libraries to augment their build-in regex 
capabilities, like e.g. [XRegExp](https://xregexp.com/))

### SIZE
The SIZE constraint validates that the size (resp. length) of the associated property value is between the number values
of the keys _min_ resp. _max_.<br>
Example:
```json
    {
      "type": "SIZE",
      "min": 0,
      "max": 10
    }
```
This constraint can be applied to properties of type:
- _string_ : the size of a string corresponds to the number of string characters.
- _array_ : the size of an array corresponds to the number of array elements.
- _object_ : the size of an object corresponds to the number of object keys.

Requirements:
- At least one of the keys _min_ or _max_ must be specified. The other key is optional.
- If both keys are specified, the _min-value_ must not be greater than the _max-value_.
- The values of the keys _min_ and _max_ must be **> 0** (zero).

### RANGE
The RANGE constraint checks whether the value of the associated property is within the range defined by the values of 
the keys _min_ and _max_.<br>
Example:
```json
    {
      "type": "RANGE",
      "min": 0,
      "max": 10
    }
```
This constraint can be applied to properties of type:
- _number_
- _string_  - as long as the string complies to the _date_ (e.g. ```"2022-12-31"```) resp. _date-time_ 
(e.g. ```"2022-12-31T23:59:59Z"```) format
(according to [RFC 3339, section 5.6](https://datatracker.ietf.org/doc/html/rfc3339#section-5.6)).

Requirements:
- the values of the keys _min_ and _max_ must equal the type of the associated property.
- At least one of the keys _min_ or _max_ must be specified. The other key is optional.<br>
- If both keys are specified, the _min-value_ must not be greater than the _max-value_.

### FUTURE_DAYS
The FUTURE_DAYS constraint checks whether the value of the associated property is a date that is at least _min_ and at most _max_ days _in the future_.<br>
The number of days are defined as values of the keys _min_ and _max_.<br>
Example:
```json
    {
      "type": "FUTURE_DAYS",
      "min": 0,
      "max": 90
    }
```
This constraint can only be applied to properties of type _string_ that complies to the _date_ (e.g. ```"2022-12-31"```) 
resp. _date-time_ (e.g. ```"2022-12-31T23:59:59Z"```) format
(according to [RFC 3339, section 5.6](https://datatracker.ietf.org/doc/html/rfc3339#section-5.6)).

Requirements:
- The key _min_ is mandatory.<br>
- The key _max_ is optional.<br>
- The values of the keys _min_ and _max_ must be **>= 0** (zero).<br>
- If both keys are specified, the _min-value_ must not be greater than the _max-value_.<br>

### PAST_DAYS
The PAST_DAYS constraint checks whether the value of the associated property is a date that is at least _min_ and at most _max_ days _in the past_.<br>
The number of days are defined as values of the keys _min_ and _max_.<br>
Example:
```json
    {
      "type": "PAST_DAYS",
      "min": 0,
      "max": 365
    }
```
This constraint can only be applied to properties of type _string_ that complies to the _date_ (e.g. ```"2022-12-31"```)
resp. _date-time_ (e.g. ```"2022-12-31T23:59:59Z"```) format
(according to [RFC 3339, section 5.6](https://datatracker.ietf.org/doc/html/rfc3339#section-5.6)).

Requirements:
- The key _min_ is mandatory.<br>
- The key _max_ is optional.<br>
- The values of the keys _min_ and _max_ must be **>= 0** (zero).<br>
- If both keys are specified, the _min-value_ must not be greater than the _max-value_.<br>

### PERIOD_DAYS
The PERIOD_DAYS constraint checks whether the value of the associated property is a date that is 0 or more days _in the
past_. The number of days is defined as value of the key _days_.<br>
Example:
```json
    {
      "type": "PERIOD_DAYS",
      "min": -365,
      "max": 365
    } 
```
This constraint can only be applied to properties of type _string_ that complies to the _date_ (e.g. ```"2022-12-31"```)
resp. _date-time_ (e.g. ```"2022-12-31T23:59:59Z"```) format
(according to [RFC 3339, section 5.6](https://datatracker.ietf.org/doc/html/rfc3339#section-5.6)).

Requirements:
- At least one of the keys _min_ or _max_ must be specified. The other key is optional.<br>
- If both keys are specified, the _min-value_ must not be greater than the _max-value_.<br>
- The values of the keys _min_ and _max_ must be **>= 0** (zero).

### WEEKDAY_ANY
The WEEKDAY_ANY constraint checks whether the value of the associated property is a date that matches any of the values
listed in the array named _days_.<br>
Example:
```json
    {
      "type": "WEEKDAY_ANY",
      "days": ["SATURDAY", "SUNDAY"],
      "nullEqualsTo": true
} 
```
This constraint can only be applied to properties of type _string_ that complies to the _date_ (e.g. ```"2022-12-31"```)
resp. _date-time_ (e.g. ```"2022-12-31T23:59:59Z"```) format
(according to [RFC 3339, section 5.6](https://datatracker.ietf.org/doc/html/rfc3339#section-5.6)).

Requirements:
- The array may contain several weekday names from this list:
  - "MONDAY",
  - "TUESDAY"
  - "WEDNESDAY"
  - "THURSDAY"
  - "FRIDAY"
  - "SATURDAY"
  - "SUNDAY"
- The array must contain at least one value.
- The optional key _nullEqualsTo_ determines how this constraint should be evaluated if the value of
  the associated property is _null_.
- For this constraint the _nullEqualsTo_ default value is _false_.

# Requirements for the implementers
An implementation of this schema must meet the following requirements. These requirements depend on which part is 
implemented.

## Producer
TODO: Describe requirements

Must provide an API to define all types of validation rules.
> E.g. with [Cross Language Validation Java](https://github.com/stephan-double-u/cross-language-validation-java)
  a mandatory rule with dependencies to other properties (see example rule 3) can be defined like this:
```java
final ValidationRules<Article> rules = new ValidationRules<>(Article.class);
rules.mandatory("responsibleUser",
        Condition.of("status", Equals.none("NEW")));
```

Must provide an API to serialize the validation rules zero or more entity types to JSON.
> E.g. [Cross Language Validation Java](https://github.com/stephan-double-u/cross-language-validation-java) provides this API:
```java
public class ValidationRules<T> {
  public static String serializeToJson(final ValidationRules<?>... rules) {
    // <details omitted>
  }
}
```

## Consumer
TODO: Describe requirements

Must provide an API to accept the JSON with the serialized validation rules.
> E.g. [CLV ECMAScript 6 implementation](https://github.com/stephan-double-u/cross-language-validation-es6) provides function:
```javascript
export function setValidationRules(rules) {}
```

## Validator
TODO: Describe requirements

### Rule validation sequence
For each property there might be _several rules_ that differ in whether and what conditions they have.
E.g. some with permission conditions defined, others not.
- First all rules whose permissions condition and property conditions are met needs to be 
determined.
- If there are no such rules all rules without any permissions condition and with matching property 
conditions needs to be determined.
- All matching rules are validated **in the order in which they are defined**, i.e. it is checked that 
the implicit constraint (for _mandatory_ and _immutable_ rules) resp. explicit constraint 
(for _content_ and _update_ rules)is fulfilled.

TODO: Example

### Validation error codes
TODO: Describe in more detail

Default error code prefixes:
- error.validation.mandatory.
- error.validation.immutable.
- error.validation.content.
- error.validation.update.

For _mandatory_ and _immutable_ rules the name of the entity type and the property name are added, e.g.
- error.validation.mandatory.article.responsibleUser

For _content_ and _update_ rules error code additionally contains the name of the constraint type in lower case, e.g.
- error.validation.content.regex_any.article.name


# Implementations
- [Cross Language Validation Java](https://github.com/stephan-double-u/cross-language-validation-java) implements a 
  Validator and a Producer for this schema in Java.
- [Cross Language Validation ECMAScript 6](https://github.com/stephan-double-u/cross-language-validation-es6) implements
  a Validator and a Consumer for this schema in ECMAScript 6.

## Implementation status
TODO Document status here?

|Supported feature | Java | ES6 |
|--- |------|-----|
|JSON Producer| +    | -   |
|JSON Consumer| -    | +   |
|Validator for _mandatory_ rules| +    | +   |
|Validator for _immutable_ rules| +    | +   |
|Validator for _content_ rules| +    | +   |
|Validator for _update_ rules| +    | +   |
|Supports simple property names (e.g. ``responsibleUser``)| +    | +   |
|Supports nested property names (e.g. ``customer.address.city``)| +    | +   |
|Supports single-indexed property names (e.g. ``medicalSets[0].articles[0].animalUse``)| +    | +   |
|Supports multi-indexed property names (e.g. ``medicalSets[1-3].articles[*].animalUse``)| +    | +   |
|Supports terminal aggregate functions| +    | +   |
|...| ?    | ?   |

# Thoughts about possible extensions
- FUTURE_HOURS etc.?<br>
  E.g. to validate that a date is at least 6 hours in the future.

- RANGE with "boundsIncluded":false?<br>
  E.g. with `"min": "5"`, to validate that a value is _is greater_ than 5<br>

- RANGE_REF?<br>
  E.g. with `"min": "intProp"`, to validate that a value is not smaller than the value of 'intProp'<br>
  or `"min": "dateProp"`, to validate that a date is not before the date of 'dateProp'

- REGEX_NONE?<br>
  E.g. to validate that a property value not match a regex

- More terminal aggregate functions?<br>
  E.g. `foo[*]#min, foo[*]#max, foo[*]#avg, foo[*]#same, foo[*]increasing, ...`

- Array index definition 'last N elements' (e.g. `[<2]`)?

- Support for 'big integer?'<br>
  see https://golb.hplar.ch/2019/01/js-bigint-json.html

- Support for 'big decimal?'<br>
  see https://stackoverflow.com/questions/16742578/bigdecimal-in-javascript

- Support for recursive properties?<br>
  E.g. for chapters with (sub-)chapters etc. Syntax?: `object[R].name"` resp. `"chapters\[*][R].name"?`
