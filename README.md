# *C*old *W*ar *Dec*oder

> A command-line tool to solve the "Operation Chaos" puzzle from *Call of Duty: Black Ops Cold War*.

## Evidence Needed

To use this tool, you need to have found the three pieces of evidence from the campaign missions:

1.  **A Coded Message (from mission "Brick in the Wall"):** This provides two number sequences (one red, one blue) with a missing number in each.
2.  **Numbers Station Broadcast (from mission "Echoes of a Cold War"):** This provides a list of cities and their corresponding number station codes.
3.  **Front Page of the Observer (from mission "Nowhere Left to Run"):** This newspaper contains a scrambled word (a city name) highlighted in red.

## Usage

You need to run the application providing the information from the evidence as command-line arguments.

### Arguments

*   `--stations` or `-s`: The list of cities and codes from the Numbers Station Broadcast.
    *   Format: `CityName1=Code1,CityName2=Code2,...`
    *   Example: `Atlanta=1234,Boston=5678`
*   `--scramble` or `-c`: The scrambled letters from the front page of the Observer newspaper.
    *   Format: A string of letters, can include spaces.
    *   Example: `"NEWYROK"`
*   `--red` or `-r`: The red number sequence from the Coded Message. Use `?` for the missing number.
    *   Format: A comma-separated list of numbers.
    *   Example: `1,2,?,4,5`
*   `--blue` or `-b`: The blue number sequence from the Coded Message. Use `?` for the missing number.
    *   Format: A comma-separated list of numbers.
    *   Example: `10,20,30,?,50`

### Example Command

With gradle:
```bash
./gradlew :app:run --args="--stations 'Houston=6195,Atlanta=6287,Tucson=6373,Miami=6459,Dallas=6625' --scramble 'SNOTOB' --red '22,23,?,27,29' --blue '53,?,57,59,61'"
```

After building a JAR (`./gradlew :app:jar`):
```bash
java -jar app/build/libs/app.jar --stations "Houston=6195,Atlanta=6287,Tucson=6373,Miami=6459,Dallas=6625" --scramble "SNOTOB" --red "22,23,?,27,29" --blue "53,?,57,59,61"
```

### Output

The tool will output:
1.  The unscrambled city and its code, which is the **first passphrase**.
2.  The missing red and blue numbers.
3.  The combined code from the missing numbers and the corresponding city, which is the **second passphrase**.

## License

This project is released under the [MIT License](https://opensource.org/licenses/MIT).
