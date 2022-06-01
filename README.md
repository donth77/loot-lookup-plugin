# Loot Lookup

RuneLite plugin to quickly reference the monster drop tables from OSRS Wiki

## Features

- "Lookup Drops" menu option
- Search bar for monster names
- Collapsible sections for each drop table
- Collapse all / Expand all button
- Display rarity as percentage
- Button to open the OSRS Wiki page for the monster in default web browser
- List and Grid views
- Links to wiki pages for items in List view
- Choose between 4 or 5 rows in Grid view
- Customize colors for rarity and prices
- Multiple tabs for monsters with separate drop tables


Config
------

![config](https://i.imgur.com/yLHZlSD.png)
#### Default view option
  - Sets the default view option on plugin startup - List or Grid
####  Rarity 
  - Show/hide rarity value in the drop tables
####  Quantity 
  - Show/hide quantity value in the drop tables
#### Price 
  - Show/hide price value in the drop tables
#### Price Type
  - Select price type for item: Grand Exchange or High Alch
#### Disable Right Click Menu Option
  - Hides the in-game right click menu option that is displayed for attackable NPCs
####  Disable Item links (List only)
  - Disables links to OSRS Wiki pages for items in List view
####  Items per row (Grid only)
  - Sets the number of items displayed in a row in Grid view
####  Common Color
  - Color to highlight the rarity of items with a value greater than 1/100
####  Rare Color
- Color to highlight the rarity of items with a value of 1/100 - 1/1000
####  Super Rare Color
- Color to highlight the rarity of items with a value of 1/1000 or less
####  Price Color
- Color to highlight item prices




Screenshots
-----------
![Giant Rat](https://i.imgur.com/kOpBmOo.png)
![Alchemical Hydra - List](https://i.imgur.com/sArKJzz.png)
![Alchemical Hydra - Grid](https://i.imgur.com/lngttYL.png)
![Black demon - tabs](https://i.imgur.com/OlSsUHR.png)

## Future

- Filter drop table sections
- Sort each section individually by item name, quantity, rarity, and price
- Replace OSRS Wiki scraping with [OSRSBox](https://www.osrsbox.com/) data pending [#161](https://github.com/osrsbox/osrsbox-db/issues/161)

## Issues

If you find any problems or have feedback, please feel free to submit an issue [here](https://github.com/donth77/loot-lookup-plugin/issues)

Credits
-------
Based on the [Loot Table](https://github.com/Sir-Kyle-Richardson/OSRS-loottable) plugin from Kyle Richardson

## Changelog
v1.0
- Initial release; added to Plugin Hub

v1.1.0
- Improve parsing for OSRS Wiki pages
- Add multiple tabs for monster pages with separate drop tables
- Improve handling of long names in List view
- Add config option to choose between 4 or 5 items in Grid view
- Add config option for common color

v1.1.1
- Fix parsing for some monsters with separate drop table tabs

v1.1.2, v1.1.3
- Auto select tab corresponding to monster level on right click menu option
- Look up monsters based on id on right click menu option

v1.1.4
- Display noted drops with a note image and include "(noted)" text in some cases

v1.1.5
- Add price type option to display GE or HA price
