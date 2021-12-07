public class Defaults {

    JSONObject data;


    // This class is responsible for grabbing static variables from JSON file.
    public Defaults() {

    }

    public JSONObject parseDefaults() {
  JSONArray a = (JSONArray) parser.parse(new FileReader("defaults.json"));

  for (Object o : a)
  {
    JSONObject person = (JSONObject) o;

    String name = (String) person.get("name");
    System.out.println(name);

    String city = (String) person.get("city");
    System.out.println(city);

    String job = (String) person.get("job");
    System.out.println(job);

    JSONArray cars = (JSONArray) person.get("cars");

    for (Object c : cars)
    {
      System.out.println(c+"");
    }
  }
    }
}
