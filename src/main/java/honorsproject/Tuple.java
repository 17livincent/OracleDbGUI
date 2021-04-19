/**
 * Tuple.java
 * @author Vincent Li <vincentl@asu.edu>
 * Represents a tuple of the Employee table.
 */
package honorsproject;

public class Tuple {
    public String empID;
    public String fName;
    public String mI;
    public String lName;

    public Tuple(String empID, String fName, String mI, String lName) {
        this.empID = empID;
        this.fName = fName;
        this.mI = mI;
        this.lName = lName;
    }

    public String[] toArray() {
        String[] array = {empID, fName, mI, lName};
        return array;
    }
}
