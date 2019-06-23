Feature: Scan
  Scan multiple shipments to validate it against requested run sheet

  Scenario: Scan multiple shipments with no discrepancy
    Given There was no discrepancy
    And Runner has finished scanning shipments
    Then  Runner should be able to start the trip

  Scenario: Scan multiple shipments with discrepancy
    Given There was a discrepancy between scanned shipments and requested sheet
    And Runner has finished scanning shipments
    Then  Runner should be navigated to discrepancy report screen