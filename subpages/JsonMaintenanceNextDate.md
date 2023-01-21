```json
{
  "schemaVersion": "0.11",
  "contentRules": {
    "article": {
      "maintenanceNextDate": [
        {
          "constraint": {
            "type": "FUTURE_DAYS",
            "min": 1,
            "max": 365,
            "nullEqualsTo": true
          },
          "permissions": {
            "type": "ANY",
            "values": [
              "MANAGER"
            ]
          }
        },
        {
          "constraint": {
            "type": "FUTURE_DAYS",
            "min": 10,
            "max": 365,
            "nullEqualsTo": true
          },
          "permissions": {
            "type": "NONE",
            "values": [
              "MANAGER"
            ]
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
