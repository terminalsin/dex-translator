package software.coley.dextransformer;

import com.android.tools.r8.graph.Code;
import com.android.tools.r8.graph.DexEncodedMethod;
import com.android.tools.r8.graph.DexProgramClass;
import com.android.tools.r8.utils.AndroidApiLevel;
import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Test;
import software.coley.dextranslator.Inputs;
import software.coley.dextranslator.Options;
import software.coley.dextranslator.model.ApplicationData;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

public class DataModelTests extends TestBase {
	@Test
	void testExportDoesNotTamperWithDataModel() {
		// Inputs
		String resourcePath = "/dx-samples/001-HelloWorld/classes.jar";
		Path jarPath = assertDoesNotThrow(() -> Paths.get(DataModelTests.class.getResource(resourcePath).toURI()));
		Inputs inputs = new Inputs().addJarArchive(jarPath);
		Options options = new Options()
				.setApiLevel(AndroidApiLevel.getAndroidApiLevel(30));

		// Model
		ApplicationData data = assertDoesNotThrow(() -> ApplicationData.from(inputs, options.getInternalOptions()));

		// Initial state
		DexProgramClass mainClass = data.getClass("Main");
		DexEncodedMethod mainMethod = Iterables.get(mainClass.methods(), 1);
		Code code = mainMethod.getCode();

		// Convert to DEX
		byte[] dexFile = assertDoesNotThrow(data::exportToDexFile);
		assertNotNull(dexFile);

		// Second export should yield same DEX
		//  - Some stuff may be out of order, so length check should suffice
		byte[] dexFileSecondPass = assertDoesNotThrow(data::exportToDexFile);
		assertEquals(dexFile.length, dexFileSecondPass.length);

		// Post-export state
		DexProgramClass mainClassPostExport = data.getClass("Main");
		DexEncodedMethod mainMethodPostExport = Iterables.get(mainClassPostExport.methods(), 1);
		Code codePostExport = mainMethodPostExport.getCode();

		// Exporting should not have affected the state
		assertSame(mainClass, mainClassPostExport);
		assertSame(mainMethod, mainMethodPostExport);
		assertSame(code, codePostExport);
	}
}
