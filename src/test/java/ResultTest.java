import org.cyphail.parser.core.Fail;
import org.cyphail.parser.core.Ok;
import org.cyphail.parser.core.Result;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ResultTest {

    @Test
    public void okGuardaTokenYResto() {
        Result<String, String, String> r = Result.ok("4", "2+3");

        assertInstanceOf(Ok.class, r);

        Ok<String, String, String> ok = (Ok<String, String, String>) r;
        assertEquals("4", ok.token());
        assertEquals("2+3", ok.rest());
    }

    @Test
    public void failGuardaRazon() {
        Result<String, String, String> r = Result.fail("Expected NUM but found 'x'");

        assertInstanceOf(Fail.class, r);

        Fail<String, String, String> fail = (Fail<String, String, String>) r;
        assertEquals("Expected NUM but found 'x'", fail.reason());
    }

    @Test
    public void switchExhaustivoDistingueCasoOk() {
        Result<String, String, String> r = Result.ok("4", "2+3");

        String salida = switch (r) {
            case Ok<String, String, String> ok -> "ok:" + ok.token();
            case Fail<String, String, String> fail -> "fail:" + fail.reason();
        };

        assertEquals("ok:4", salida);
    }

    @Test
    public void switchExhaustivoDistingueCasoFail() {
        Result<String, String, String> r = Result.fail("error de sintaxis");

        String salida = switch (r) {
            case Ok<String, String, String> ok -> "ok:" + ok.token();
            case Fail<String, String, String> fail -> "fail:" + fail.reason();
        };

        assertEquals("fail:error de sintaxis", salida);
    }
}