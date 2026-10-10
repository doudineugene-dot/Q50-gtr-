import java.io.*;
import ru.q50.codex.ProbeIO;
public class ProbeChecks {
    private static int count;
    static void check(boolean b){count++;if(!b)throw new AssertionError("check "+count);}
    public static void main(String[] args)throws Exception {
        check(ProbeIO.relevant("com.ygomi.packageprovider"));check(ProbeIO.relevant("VR30 DASH"));
        check(ProbeIO.relevant("tel_proc"));check(!ProbeIO.relevant("ordinary.package"));
        check(ProbeIO.bounded(new ByteArrayInputStream(new byte[]{65,0,66}),4).equals("A B"));
        check(ProbeIO.bounded(new ByteArrayInputStream(new byte[200]),100).endsWith("TRUNCATED_AT 100"));
        check(ProbeIO.bounded(new ByteArrayInputStream(new byte[0]),100).equals(""));
        check(ProbeIO.read(new File("missing-probe-file")).startsWith("READ_FAILED"));
        check(ProbeIO.methods(ProbeIO.class).contains("bounded"));
        System.out.println("PASS: "+count+" probe evidence boundary checks");
    }
}
