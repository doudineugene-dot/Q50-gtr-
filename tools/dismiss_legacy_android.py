"""Accept measured compatibility dialogs on the CI emulator only."""
import pathlib, re, subprocess, sys, time, xml.etree.ElementTree as ET

out=pathlib.Path(sys.argv[1]);out.mkdir(parents=True,exist_ok=True)
for index in range(3):
    time.sleep(1)
    subprocess.run(["adb","shell","uiautomator","dump","/sdcard/q50-compat.xml"],check=True,capture_output=True)
    xml=subprocess.check_output(["adb","exec-out","cat","/sdcard/q50-compat.xml"])
    (out/f"compat-{index}.xml").write_bytes(xml)
    button=next((n for n in ET.fromstring(xml).iter("node") if n.get("resource-id") in (
        "com.android.permissioncontroller:id/continue_button","android:id/button1")),None)
    if button is None:break
    x1,y1,x2,y2=map(int,re.findall(r"\d+",button.get("bounds")))
    subprocess.run(["adb","shell","input","tap",str((x1+x2)//2),str((y1+y2)//2)],check=True)
