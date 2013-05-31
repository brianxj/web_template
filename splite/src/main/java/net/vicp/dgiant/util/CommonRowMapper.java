package net.vicp.dgiant.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import net.vicp.dgiant.exception.RawResultPaginationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.support.DatabaseResults;

public class CommonRowMapper<T> implements RowMapper<T> {

	private Logger logger = (Logger) LoggerFactory
			.getLogger(CommonRowMapper.class);

	private Class<T> clazz;

	private Field[] fields;

	private List<String> columns;

	public CommonRowMapper(Class<T> clazz) {
		this.clazz = clazz;
		this.fields = clazz.getDeclaredFields();
	}

	@Override
	public T mapRow(DatabaseResults rs) throws RawResultPaginationException {

		T entry;
		try {
			entry = clazz.newInstance();

			if (columns == null) {
				columns = Arrays.asList(rs.getColumnNames());
			}

			for (Field field : fields) {

				DatabaseField dbFiled = field
						.getAnnotation(DatabaseField.class);
				String columnName = "".equals(dbFiled.columnName()) ? field
						.getName() : dbFiled.columnName();

				int index = columns.indexOf(columnName);

				if (index == -1) {
					continue;
				}

				Method setterMethod = clazz.getMethod(setter(field),
						field.getType());

				if (Integer.class == field.getType() || "int" == field.getType().getName()) {

					setterMethod.invoke(entry, rs.getInt(index));
					continue;

				} else if (String.class == field.getType()) {

					setterMethod.invoke(entry, rs.getString(index));
					continue;

				} else if (Date.class == field.getType()) {

					if (DataType.DATE_STRING == dbFiled.dataType()) {

						setterMethod.invoke(entry, new SimpleDateFormat(
								Constants.DEFAULT_DATE_FORMAT).parse(rs
								.getString(index)));

					} else if (DataType.DATE_LONG == dbFiled.dataType()) {

						setterMethod.invoke(entry,
								new Date(1000 * rs.getLong(index)));

					}
					continue;

				} else {

					// currently the common RowMapper does not support the entry
					// who want to load its foreign member
					logger.warn(field.getType().getName()
							+ " is not supported by CommonRowMapper");

				}
			}
		} catch (Exception e) {

			logger.error(e.getMessage());

			throw new RawResultPaginationException(e.getMessage());
		}

		return entry;
	}

	/**
	 * setter method name of the field
	 * 
	 * @param field
	 * @return
	 */
	private String setter(Field field) {

		String name = field.getName();

		if (name.length() == 1) {
			return "set" + name.toUpperCase();
		}

		return "set" + name.toUpperCase().charAt(0) + name.substring(1);
	}

}
