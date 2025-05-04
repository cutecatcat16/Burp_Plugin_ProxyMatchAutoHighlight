# Plugin function

## ProxyMatcher
1. Proxy match using regex.
2. Matched paths will be highlighted.
3. Main purpose of this function will be to highlight added endpoints as a means to mark endpoints as tested or completed.

## API Mapper
1. Auto populate endpoint paths into a table.
2. Main purpose of this function will be for mapping out an entire application during APT.

# Updates 

### 4 May 2025
* Added Sort function to remove duplicates to the ProxyMatcher function. Now we do not need to remove all endpoints and re-paste contents to remove duplicates.
* Added New core function API Mapper. This function uses a table and get the endpoint paths from proxy history and populate the table. We have added copy list, sort list, remove endpoints, remove endpoints with extensions to this function
  * Copy list: to copy the entire table content in one click.
  * Sort list: does not sort according to alphabetic order but to remove duplicates. So we have a clean list.
  * Remove endpoints: Remove selected row values/endpoints.
  * Remove endpoints with extensions: A dialog prompt will be triggered and user have to input the extensions (one per line) to remove from the table. This uses endsWith method to check the entire path (queries are removed by default) and remove if it matches.

--- 

