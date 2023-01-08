```json
{
  "schemaVersion": "0.10",
  "contentRules": {
    "article": {
      "maintenanceNextDate": [
        {
          "constraint": {
            "type": "FUTURE_DAYS",
            "min": 1,
            "max": 365
          },
          "permissions": {
            "type": "ANY",
            "values": [
              "MANAGER"
            ]
          },
          "condition": {
            "property": "maintenanceNextDate",
            "constraint": {
              "type": "EQUALS_NOT_NULL"
            }
          }
        },
        {
          "constraint": {
            "type": "FUTURE_DAYS",
            "min": 10,
            "max": 365
          },
          "permissions": {
            "type": "NONE",
            "values": [
              "MANAGER"
            ]
          },
          "condition": {
            "property": "maintenanceNextDate",
            "constraint": {
              "type": "EQUALS_NOT_NULL"
            }
          }
        },
        {
          "constraint": {
            "type": "WEEKDAY_ANY",
            "days": [
              "MONDAY",
              "TUESDAY",
              "WEDNESDAY",
              "THURSDAY",
              "FRIDAY"
            ],
            "nullEqualsTo": true
          }
        }
      ]
    }
  }
}
```
