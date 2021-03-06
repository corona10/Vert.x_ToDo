package todo;

import java.sql.SQLException;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import todo.ToDoModel;

public class ToDoDatabase {

	public static ConnectionSource connectionSource;

	static void init_db(String db_url) {
		connectionSource = null;
		try {
			connectionSource = new JdbcConnectionSource(db_url);
			TableUtils.createTableIfNotExists(connectionSource, ToDoModel.class);

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (connectionSource != null) {
				try {
					connectionSource.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
