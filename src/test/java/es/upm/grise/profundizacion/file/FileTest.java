
package es.upm.grise.profundizacion.file;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.zip.CRC32;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import es.upm.grise.profundizacion.exceptions.EmptyBytesArrayException;
import es.upm.grise.profundizacion.exceptions.InvalidContentException;
import es.upm.grise.profundizacion.exceptions.WrongFileTypeException;

public class FileTest {

	@Test
	@DisplayName("addProperty añade el contenido al final del content existente")
	void testAddPropertyAddsContent() throws InvalidContentException, WrongFileTypeException {
		File file = new File();
		file.setType(FileType.PROPERTY);

		char[] first = "DATE=".toCharArray();
		char[] second = "20250919".toCharArray();

		file.addProperty(first);
		file.addProperty(second);

		List<Character> content = file.getContent();
		assertEquals(first.length + second.length, content.size(), "Debe haberse concatenado el contenido");

		int idx = 0;
		for (char c : first) assertEquals(c, content.get(idx++), "Secuencia incorrecta tras la primera inserción");
		for (char c : second) assertEquals(c, content.get(idx++), "Secuencia incorrecta tras la segunda inserción");
	}

	@Test
	@DisplayName("addProperty lanza InvalidContentException si newcontent es null")
	void testAddPropertyThrowsInvalidContentException() {
		File file = new File();
		file.setType(FileType.PROPERTY);
		assertThrows(InvalidContentException.class, () -> file.addProperty(null));
	}

	@Test
	@DisplayName("addProperty lanza WrongFileTypeException cuando el tipo es IMAGE")
	void testAddPropertyThrowsWrongFileTypeException() {
		File file = new File();
		file.setType(FileType.IMAGE);
		char[] any = "KEY=VALUE".toCharArray();
		assertThrows(WrongFileTypeException.class, () -> file.addProperty(any));
	}

	@Test
	@DisplayName("getCRC32 devuelve 0 cuando el content está vacío")
	void testGetCRC32ReturnsZeroWhenEmpty() throws EmptyBytesArrayException {
		File file = new File();
		long crc = file.getCRC32();
		assertEquals(0L, crc, "Con contenido vacío debe devolver 0");
	}


	@Test
	@DisplayName("getCRC32 delega correctamente en FileUtils y devuelve su resultado")
	void testGetCRC32CalculatesCorrectValue() throws Exception {
		File file = new File();
		file.setType(FileType.PROPERTY);
		char[] payload = "AB".toCharArray();
		file.addProperty(payload);

		long mockedCRC = 987654321L;

		try (MockedConstruction<FileUtils> mocked = mockConstruction(FileUtils.class,
				(mock, context) -> when(mock.calculateCRC32(any(byte[].class))).thenReturn(mockedCRC))) {

			long actual = file.getCRC32();

			// Verifica que devuelve el valor del mock
			assertEquals(mockedCRC, actual, "Debe devolver el valor proporcionado por FileUtils");

			// Verifica que se creó una instancia de FileUtils
			assertEquals(1, mocked.constructed().size(), "Debe crearse exactamente una instancia de FileUtils");

			// Captura el argumento para comprobar tamaño correcto
			verify(mocked.constructed().get(0)).calculateCRC32(argThat(bytes -> bytes.length == payload.length * 2));
		}
	}

	@Test
	@DisplayName("getCRC32 delega en FileUtils.calculateCRC32 (usando Mockito para interceptar la construcción)")
	void testGetCRC32UsesFileUtils() throws Exception {
		File file = new File();
		file.setType(FileType.PROPERTY);
		file.addProperty("X".toCharArray()); // asegurar contenido no vacío

		long forcedCrc = 123456789L;

		try (MockedConstruction<FileUtils> mocked = mockConstruction(
				FileUtils.class,
				(mock, context) -> when(mock.calculateCRC32(any(byte[].class))).thenReturn(forcedCrc))) {

			long crc = file.getCRC32();

			assertEquals(forcedCrc, crc, "Debe devolver el valor proporcionado por FileUtils");
			assertEquals(1, mocked.constructed().size(), "Debe crearse exactamente una instancia de FileUtils");
			verify(mocked.constructed().get(0), atLeastOnce()).calculateCRC32(any(byte[].class));
		}
	}
}
