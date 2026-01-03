# TODO 
- [ ] Encrypt strings stored in datastore.  
- [ ] Get rid of web tokens and implement mobile client PKCE auth.

# Database migrations
[docs](https://sqldelight.github.io/sqldelight/2.1.0/multiplatform_sqlite/migrations/)
- Create a db with the initial schema:
```
./gradlew generateCommonMainDatabaseSchema
```
This should be already inside the app under `src/commonMain/sqldelight/databases`
- Create a migration under `src/commonMain/sqldelight/migrations/<version to upgrate from>.sqm`
- Alter your tables under `<Entity>.sq` as needed
> Important: pay attention to the ordinal position of newly added columns
- Verify the migration using 
```
./gradlew verifySqlDelightMigration
```
# Attributions 

## Icons
[Alone icons created by AbtoCreative - Flaticon](https://www.flaticon.com/free-icons/alone)

[Letter icons created by Freepik - Flaticon](https://www.flaticon.com/free-icons/letter)

[Contact book icons created by Soremba - Flaticon](https://www.flaticon.com/free-icons/contact-book)

<a href="https://www.flaticon.com/free-icons/soccer" title="soccer icons">Soccer icons created by andinur - Flaticon</a>