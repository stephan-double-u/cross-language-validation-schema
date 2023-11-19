```json
{
  "schemaVersion": "0.12",
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
                    "property": "medicalSet",
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
                    "property": "animalUse",
                    "constraint": {
                      "type": "EQUALS_ANY",
                      "values": [
                        true
                      ]
                    }
                  },
                  {
                    "property": "everLeftWarehouse",
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
