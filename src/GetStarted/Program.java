package GetStarted;

import static  java.lang.System.*;

import java.io.IOException;
import java.util.Iterator;

import com.microsoft.azure.documentdb.*;

public class Program {

    private DocumentClient client;

    /**
     * Run a Hello DocumentDB console application.
     * 
     * @param args
     *            command line arguments
     * @throws DocumentClientException
     *             exception
     * @throws IOException 
     */
    public static void main(String[] args) {

        try {
            Program p = new Program();
            p.getStartedDemo();
            System.out.println(String.format("Demo complete, please hold while resources are deleted"));
        } catch (Exception e) {
            System.out.println(String.format("DocumentDB GetStarted failed with %s", e));
        }
    }

    private void getStartedDemo() throws DocumentClientException, IOException {
        this.client = new DocumentClient("https://kaix-sql.documents.azure.com:443/",
                "mI7GQzpfrkZIC4Wy8BdBmSktYhJCC9n5HWMW0cHKRlpjDj1zaXixuIKqfkYpJXkUi4rCkQhVifYRAkj1pjIjcQ==",
                new ConnectionPolicy(),
                ConsistencyLevel.Session);

        this.createDatabaseIfNotExists("FamilyDB");
        this.createDocumentCollectionIfNotExists("FamilyDB", "FamilyCollection");

        Family andersenFamily = getAndersenFamilyDocument();
        this.createFamilyDocumentIfNotExists("FamilyDB", "FamilyCollection", andersenFamily);

        Family wakefieldFamily = getWakefieldFamilyDocument();
        this.createFamilyDocumentIfNotExists("FamilyDB", "FamilyCollection", wakefieldFamily);

        this.executeSimpleQuery("FamilyDB", "FamilyCollection");

        this.client.deleteDatabase("/dbs/FamilyDB", null);
    }

    private Family getAndersenFamilyDocument() {
        Family andersenFamily = new Family();
        andersenFamily.setId("Andersen.1");
        andersenFamily.setLastName("Andersen");

        Parent parent1 = new Parent();
        parent1.setFirstName("Thomas");

        Parent parent2 = new Parent();
        parent2.setFirstName("Mary Kay");

        andersenFamily.setParents(new Parent[] { parent1, parent2 });

        Child child1 = new Child();
        child1.setFirstName("Henriette Thaulow");
        child1.setGender("female");
        child1.setGrade(5);

        Pet pet1 = new Pet();
        pet1.setGivenName("Fluffy");

        child1.setPets(new Pet[] { pet1 });

        andersenFamily.setDistrict("WA5");
        Address address = new Address();
        address.setCity("Seattle");
        address.setCounty("King");
        address.setState("WA");

        andersenFamily.setAddress(address);
        andersenFamily.setRegistered(true);

        return andersenFamily;
    }

    private Family getWakefieldFamilyDocument() {
        Family wakefieldFamily = new Family();
        wakefieldFamily.setId("Wakefield.7");
        wakefieldFamily.setLastName("Wakefield");

        Parent parent1 = new Parent();
        parent1.setFamilyName("Wakefield");
        parent1.setFirstName("Robin");

        Parent parent2 = new Parent();
        parent2.setFamilyName("Miller");
        parent2.setFirstName("Ben");

        wakefieldFamily.setParents(new Parent[] { parent1, parent2 });

        Child child1 = new Child();
        child1.setFirstName("Jesse");
        child1.setFamilyName("Merriam");
        child1.setGrade(8);

        Pet pet1 = new Pet();
        pet1.setGivenName("Goofy");

        Pet pet2 = new Pet();
        pet2.setGivenName("Shadow");

        child1.setPets(new Pet[] { pet1, pet2 });

        Child child2 = new Child();
        child2.setFirstName("Lisa");
        child2.setFamilyName("Miller");
        child2.setGrade(1);
        child2.setGender("female");

        wakefieldFamily.setChildren(new Child[] { child1, child2 });

        Address address = new Address();
        address.setCity("NY");
        address.setCounty("Manhattan");
        address.setState("NY");

        wakefieldFamily.setAddress(address);
        wakefieldFamily.setDistrict("NY23");
        wakefieldFamily.setRegistered(true);
        return wakefieldFamily;
    }

    private void createDatabaseIfNotExists(String databaseName) throws DocumentClientException, IOException {
        String databaseLink = String.format("/dbs/%s", databaseName);

        // Check to verify a database with the id=FamilyDB does not exist
        try {
            this.client.readDatabase(databaseLink, null);
            this.writeToConsoleAndPromptToContinue(String.format("Found %s", databaseName));
        } catch (DocumentClientException de) {
            // If the database does not exist, create a new database
            if (de.getStatusCode() == 404) {
                Database database = new Database();
                database.setId(databaseName);
                
                this.client.createDatabase(database, null);
                this.writeToConsoleAndPromptToContinue(String.format("Created %s", databaseName));
            } else {
                throw de;
            }
        }
    }

    private void createDocumentCollectionIfNotExists(String databaseName, String collectionName) throws IOException,
            DocumentClientException {
        String databaseLink = String.format("/dbs/%s", databaseName);
        String collectionLink = String.format("/dbs/%s/colls/%s", databaseName, collectionName);

        try {
            this.client.readCollection(collectionLink, null);
            writeToConsoleAndPromptToContinue(String.format("Found %s", collectionName));
        } catch (DocumentClientException de) {
            // If the document collection does not exist, create a new
            // collection
            if (de.getStatusCode() == 404) {
                DocumentCollection collectionInfo = new DocumentCollection();
                collectionInfo.setId(collectionName);

                // Optionally, you can configure the indexing policy of a
                // collection. Here we configure collections for maximum query
                // flexibility including string range queries.
                RangeIndex index = new RangeIndex(DataType.String);
                index.setPrecision(-1);

                collectionInfo.setIndexingPolicy(new IndexingPolicy(new Index[] { index }));

                // DocumentDB collections can be reserved with throughput
                // specified in request units/second. 1 RU is a normalized
                // request equivalent to the read of a 1KB document. Here we
                // create a collection with 400 RU/s.
                RequestOptions requestOptions = new RequestOptions();
                requestOptions.setOfferThroughput(400);

                this.client.createCollection(databaseLink, collectionInfo, requestOptions);

                this.writeToConsoleAndPromptToContinue(String.format("Created %s", collectionName));
            } else {
                throw de;
            }
        }

    }

    private void createFamilyDocumentIfNotExists(String databaseName, String collectionName, Family family)
            throws DocumentClientException, IOException {
        try {
            String documentLink = String.format("/dbs/%s/colls/%s/docs/%s", databaseName, collectionName, family.getId());
            this.client.readDocument(documentLink, new RequestOptions());
        } catch (DocumentClientException de) {
            if (de.getStatusCode() == 404) {
                String collectionLink = String.format("/dbs/%s/colls/%s", databaseName, collectionName);
                this.client.createDocument(collectionLink, family, new RequestOptions(), true);
                this.writeToConsoleAndPromptToContinue(String.format("Created Family %s", family.getId()));
            } else {
                throw de;
            }
        }
    }

    private void executeSimpleQuery(String databaseName, String collectionName) {
        // Set some common query options
        FeedOptions queryOptions = new FeedOptions();
        queryOptions.setPageSize(-1);
        queryOptions.setEnableCrossPartitionQuery(true);

        RequestOptions requestOptions = new RequestOptions();

        String collectionLink = String.format("/dbs/%s/colls/%s", databaseName, collectionName);
        String collectionResourceId = null;
        int newThroughput = 600;
        try {
            collectionResourceId = client.readCollection(collectionLink, requestOptions).getResource().getResourceId();


        Iterator<Offer> it = client.queryOffers(
                String.format("SELECT * FROM r where r.offerResourceId = '%s'", collectionResourceId), null).getQueryIterator();


        Offer offer = it.next();
        out.println("The offer now is "+offer.getContent().getInt("offerThroughput")+". update it to "+newThroughput);

        // update the offer

        offer.getContent().put("offerThroughput", newThroughput);
        client.replaceOffer(offer);

        int theneww = client.queryOffers(String.format("SELECT * FROM r where r.offerResourceId = '%s'",
                collectionResourceId), null).getQueryIterator().next().getContent().getInt("offerThroughput");

        out.println("The new throughput is "+theneww);

        } catch (DocumentClientException e) {
            e.printStackTrace();
        }


        FeedResponse<Document> queryResults = this.client.queryDocuments(collectionLink,
                "SELECT * FROM Family WHERE Family.lastName = 'Andersen'", queryOptions);

        System.out.println("Running SQL query...");
        System.out.println("The Request charge （RU）for this request is "+queryResults.getRequestCharge()+"RU");

        for (Document family : queryResults.getQueryIterable()) {
            System.out.println(String.format("\tRead %s", family));
        }
    }

    @SuppressWarnings("unused")
    private void replaceFamilyDocument(String databaseName, String collectionName, String familyName, Family updatedFamily)
            throws IOException, DocumentClientException {
        try {
            this.client.replaceDocument(
                    String.format("/dbs/%s/colls/%s/docs/%s", databaseName, collectionName, updatedFamily.getId()), updatedFamily,
                    null);
            writeToConsoleAndPromptToContinue(String.format("Replaced Family %s", updatedFamily.getId()));
        } catch (DocumentClientException de) {
            throw de;
        }
    }

    @SuppressWarnings("unused")
    private void deleteFamilyDocument(String databaseName, String collectionName, String documentName) throws IOException,
            DocumentClientException {
        try {
            this.client.deleteDocument(String.format("/dbs/%s/colls/%s/docs/%s", databaseName, collectionName, documentName), null);
            writeToConsoleAndPromptToContinue(String.format("Deleted Family %s", documentName));
        } catch (DocumentClientException de) {
            long time = Long.parseLong( de.getResponseHeaders().get("x-ms-retry-after-ms"));
            throw de;
        }
    }

    private void writeToConsoleAndPromptToContinue(String text) throws IOException {
        System.out.println(text);
        System.out.println("Press any key to continue ...");
        System.in.read();
    }


}

