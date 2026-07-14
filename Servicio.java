/*
  Servicio.java
  Servicio web REST
*/

package servicio;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Application;

import java.sql.*;
import javax.sql.DataSource;
import javax.naming.Context;
import javax.naming.InitialContext;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.security.SecureRandom;

import com.fasterxml.jackson.databind.ObjectMapper;

/*
 La URL del servicio web es http://localhost:8080/Servicio/rest/ws
 Donde:
 Servicio: dominio del servicio web (es decir, el nombre de archivo Servicio.war)
 rest: se define en la etiqueta <url-pattern> de <servlet-mapping> en el archivo WEB-INF\web.xml
 ws: se define en la siguiente anotación @Path de la clase Servicio
*/

@Path("ws")
public class Servicio extends Application
{
  static DataSource pool = null;
  static
  {		
    try
    {
      Context ctx = new InitialContext();
      pool = (DataSource)ctx.lookup("java:comp/env/jdbc/datasource_Servicio");
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
  }

  static ObjectMapper j = new ObjectMapper().setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS"));

  static final String caracteres = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
  static final SecureRandom random = new SecureRandom();

  static String generarToken(int longitud)
  {
    StringBuilder sb = new StringBuilder(longitud);
    for (int i = 0; i < longitud; i++)
    {
      int index = random.nextInt(caracteres.length());
      sb.append(caracteres.charAt(index));
    }
    return sb.toString();
  }

  boolean verifica_acceso(Connection conexion,int id_usuario,String token) throws Exception
  {
    PreparedStatement stmt_1 = conexion.prepareStatement("SELECT 1 FROM usuarios WHERE id_usuario=? and token=?");
    try
    {
      stmt_1.setInt(1,id_usuario);
      stmt_1.setString(2,token);

      ResultSet rs = stmt_1.executeQuery();
      try
      {
        return rs.next();
      }
      finally
      {
        rs.close();
      }
    }
    finally
    {
      stmt_1.close();
    }
  }

  @GET
  @Path("login")
  @Produces(MediaType.APPLICATION_JSON)
  public Response login(@QueryParam("email") String email,@QueryParam("password") String password) throws Exception
  {
    try
    {
      Connection conexion= pool.getConnection();

      try
      {
        PreparedStatement stmt_1 = conexion.prepareStatement("SELECT id_usuario FROM usuarios WHERE email=? and password=?");
        try
        {
          stmt_1.setString(1,email);
          stmt_1.setString(2,password);

          ResultSet rs = stmt_1.executeQuery();
          try
          {
            if (rs.next())
            {
              int id_usuario = rs.getInt(1);
              String token = generarToken(20);

              PreparedStatement stmt_2 = conexion.prepareStatement("UPDATE usuarios SET token=? WHERE id_usuario=?");
              try
              {
                stmt_2.setString(1,token);
                stmt_2.setInt(2,id_usuario);
                stmt_2.executeUpdate();
              }
              finally
              {
                stmt_2.close();
              }

              return Response.ok("{\"id_usuario\":" + id_usuario + "," + "\"token\":\"" + token + "\"}").build();
            }
            return Response.status(400).entity(j.writeValueAsString(new Respuesta("Acceso denegado"))).build();
          }
          finally
          {
            rs.close();
          }
        }
        finally
        {
          stmt_1.close();
        }
      }
      finally
      {
        conexion.close();
      }
    }
    catch (Exception e)
    {
      return Response.status(400).entity(j.writeValueAsString(new Respuesta(e.getMessage()))).build();
    }
  }

  @POST
  @Path("alta_usuario")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response alta(Usuario usuario) throws Exception
  {
    try
    {
      Connection conexion = pool.getConnection();
      int id_usuario = 0;

      if (usuario.email == null || usuario.email.equals(""))
        throw new Exception("Se debe ingresar el email");

      if (usuario.password == null || usuario.password.equals(""))
        throw new Exception("Se debe ingresar la contraseña");

      if (usuario.nombre == null || usuario.nombre.equals(""))
        throw new Exception("Se debe ingresar el nombre");

      if (usuario.apellido_paterno == null || usuario.apellido_paterno.equals(""))
        throw new Exception("Se debe ingresar el apellido paterno");

      if (usuario.fecha_nacimiento == null)
        throw new Exception("Se debe ingresar la fecha de nacimiento");

      try
      {
        conexion.setAutoCommit(false);

        PreparedStatement stmt_1 = conexion.prepareStatement("INSERT INTO usuarios(id_usuario,email,password,nombre,apellido_paterno,apellido_materno,fecha_nacimiento,telefono,genero) VALUES (0,?,?,?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS);

        try
        {
          stmt_1.setString(1,usuario.email);
          stmt_1.setString(2,usuario.password);
          stmt_1.setString(3,usuario.nombre);
          stmt_1.setString(4,usuario.apellido_paterno);

          if (usuario.apellido_materno != null)
            stmt_1.setString(5,usuario.apellido_materno);
          else
            stmt_1.setNull(5,Types.VARCHAR);

          stmt_1.setTimestamp(6,usuario.fecha_nacimiento);

          if (usuario.telefono != null)
            stmt_1.setLong(7,usuario.telefono);
          else
            stmt_1.setNull(7,Types.BIGINT);

          if (usuario.genero != null)
            stmt_1.setString(8,usuario.genero);
          else
            stmt_1.setNull(8,Types.CHAR);

          stmt_1.executeUpdate();
        
          ResultSet rs = stmt_1.getGeneratedKeys();

          try
          {
            if (rs.next())
              id_usuario = rs.getInt(1); // Obtiene el ID del usuario que se insertó previamente
          }
          finally
          {
            rs.close();
          }

          if (id_usuario == 0)
            return Response.status(400).entity(j.writeValueAsString(new Respuesta("No se pudo obtener el ID del usuario"))).build();
        }
        finally
        {
          stmt_1.close();
        }

        if (usuario.foto != null)
        {
          PreparedStatement stmt_2 = conexion.prepareStatement("INSERT INTO fotos_usuarios(id_foto,foto,id_usuario) VALUES (0,?,?)");

          try
          {
            stmt_2.setBytes(1,usuario.foto);
            stmt_2.setInt(2,id_usuario);
            stmt_2.executeUpdate();
          }
          finally
          {
            stmt_2.close();
          }
        }
        conexion.commit();
      }
      catch (Exception e)
      {
        conexion.rollback();
        throw new Exception(e.getMessage());
      }
      finally
      {
        conexion.setAutoCommit(true);
        conexion.close();
      }
      return Response.ok(j.writeValueAsString(new Respuesta("Se dio de alta el usuario"))).build();
    }
    catch (Exception e)
    {
      return Response.status(400).entity(j.writeValueAsString(new Respuesta(e.getMessage()))).build();
    }
  }

  @GET
  @Path("consulta_usuario")
  @Produces(MediaType.APPLICATION_JSON)
  public Response consulta(@QueryParam("id_usuario") int id_usuario,@QueryParam("token") String token) throws Exception
  {
    try
    {
      Connection conexion= pool.getConnection();

      if (!verifica_acceso(conexion,id_usuario,token))
        return Response.status(400).entity(j.writeValueAsString(new Respuesta("Acceso denegado"))).build();

      try
      {
        PreparedStatement stmt_1 = conexion.prepareStatement("SELECT a.email,a.nombre,a.apellido_paterno,a.apellido_materno,a.fecha_nacimiento,a.telefono,a.genero,b.foto FROM usuarios a LEFT OUTER JOIN fotos_usuarios b ON a.id_usuario=b.id_usuario WHERE a.id_usuario=?");
        try
        {
          stmt_1.setInt(1,id_usuario);

          ResultSet rs = stmt_1.executeQuery();

          try
          {
            if (rs.next())
            {
              Usuario r = new Usuario();
              r.email = rs.getString(1);
              r.nombre = rs.getString(2);
              r.apellido_paterno = rs.getString(3);
              r.apellido_materno = rs.getString(4);
              r.fecha_nacimiento = rs.getTimestamp(5);
              r.telefono = rs.getObject(6) != null ? rs.getLong(6) : null;
              r.genero = rs.getString(7);
	      r.foto = rs.getBytes(8);
              return Response.ok().entity(j.writeValueAsString(r)).build();
            }
            return Response.status(400).entity(j.writeValueAsString(new Respuesta("Acceso denegado"))).build();
          }
          finally
          {
            rs.close();
          }
        }
        finally
        {
          stmt_1.close();
        }
      }
      finally
      {
        conexion.close();
      }
    }
    catch (Exception e)
    {
      return Response.status(400).entity(j.writeValueAsString(new Respuesta(e.getMessage()))).build();
    }
  }

  @PUT
  @Path("modifica_usuario")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response modifica(@QueryParam("id_usuario") int id_usuario,@QueryParam("token") String token,Usuario usuario) throws Exception
  {
    try
    {
      Connection conexion= pool.getConnection();

      if (!verifica_acceso(conexion,id_usuario,token))
        return Response.status(400).entity(j.writeValueAsString(new Respuesta("Acceso denegado"))).build();

      if (usuario.email == null || usuario.email.equals(""))
        throw new Exception("Se debe ingresar el email");

      if (usuario.nombre == null || usuario.nombre.equals(""))
        throw new Exception("Se debe ingresar el nombre");

      if (usuario.apellido_paterno == null || usuario.apellido_paterno.equals(""))
        throw new Exception("Se debe ingresar el apellido paterno");

      if (usuario.fecha_nacimiento == null)
        throw new Exception("Se debe ingresar la fecha de nacimiento");

      conexion.setAutoCommit(false);

      try
      {
        PreparedStatement stmt_1 = conexion.prepareStatement("UPDATE usuarios SET email=?,nombre=?,apellido_paterno=?,apellido_materno=?,fecha_nacimiento=?,telefono=?,genero=? WHERE id_usuario=?");

        try
        {
          stmt_1.setString(1,usuario.email);
          stmt_1.setString(2,usuario.nombre);
          stmt_1.setString(3,usuario.apellido_paterno);

          if (usuario.apellido_materno != null)
            stmt_1.setString(4,usuario.apellido_materno);
          else
            stmt_1.setNull(4,Types.VARCHAR);

          stmt_1.setTimestamp(5,usuario.fecha_nacimiento);

          if (usuario.telefono != null)
            stmt_1.setLong(6,usuario.telefono);
          else
            stmt_1.setNull(6,Types.BIGINT);

          if (usuario.genero != null)
            stmt_1.setString(7,usuario.genero);
          else
            stmt_1.setNull(7,Types.CHAR);

          stmt_1.setInt(8,id_usuario);

          stmt_1.executeUpdate();
        }
        finally
        {
          stmt_1.close();
        }

        if (!usuario.password.equals(""))
        {
          PreparedStatement stmt_2 = conexion.prepareStatement("UPDATE usuarios SET password=? WHERE id_usuario=?");

          try
          {
            stmt_2.setString(1,usuario.password);
            stmt_2.setInt(2,id_usuario);
            stmt_2.executeUpdate();
          }
          finally
          {
            stmt_2.close();
          }
        }

        PreparedStatement stmt_3 = conexion.prepareStatement("DELETE FROM fotos_usuarios WHERE id_usuario=?");

        try
        {
          stmt_3.setInt(1,id_usuario);
          stmt_3.executeUpdate();
        }
        finally
        {
          stmt_3.close();
        }

        if (usuario.foto != null)
        {
          PreparedStatement stmt_4 = conexion.prepareStatement("INSERT INTO fotos_usuarios(id_foto,foto,id_usuario) VALUES (0,?,?)");

          try
          {
            stmt_4.setBytes(1,usuario.foto);
            stmt_4.setInt(2,id_usuario);
            stmt_4.executeUpdate();
          }
          finally
          {
            stmt_4.close();
          }
        }

        conexion.commit();
        return Response.ok(j.writeValueAsString(new Respuesta("Se modificó el usuario"))).build();      
      }
      catch (Exception e)
      {
        conexion.rollback();
        throw new Exception (e.getMessage());
      }
      finally
      {
        conexion.setAutoCommit(true);
        conexion.close();
      }
    }
    catch (Exception e)
    {
      return Response.status(400).entity(j.writeValueAsString(new Respuesta(e.getMessage()))).build();
    }
  }

  @DELETE
  @Path("borra_usuario")
  @Produces(MediaType.APPLICATION_JSON)
  public Response borra(@QueryParam("id_usuario") int id_usuario,@QueryParam("token") String token) throws Exception
  {
    try
    {
      Connection conexion= pool.getConnection();

      if (!verifica_acceso(conexion,id_usuario,token))
        return Response.status(400).entity(j.writeValueAsString(new Respuesta("Acceso denegado"))).build();

      try
      {
        PreparedStatement stmt_1 = conexion.prepareStatement("SELECT 1 FROM usuarios WHERE id_usuario=?");

        try
        {
          stmt_1.setInt(1,id_usuario);

          ResultSet rs = stmt_1.executeQuery();

          try
          {
            if (!rs.next())
              return Response.status(400).entity(j.writeValueAsString(new Respuesta("El email no existe"))).build();
          }
          finally
          {
            rs.close();
          }
        }
        finally
        {
          stmt_1.close();
        }
        conexion.setAutoCommit(false);
        PreparedStatement stmt_2 = conexion.prepareStatement("DELETE FROM fotos_usuarios WHERE id_usuario=?");

        try
        {
          stmt_2.setInt(1,id_usuario);
          stmt_2.executeUpdate();
        }
        finally
        {
          stmt_2.close();
        }

        PreparedStatement stmt_3 = conexion.prepareStatement("DELETE FROM usuarios WHERE id_usuario=?");

        try
        {
          stmt_3.setInt(1,id_usuario);
          stmt_3.executeUpdate();
        }
        finally
        {
          stmt_3.close();
        }
        conexion.commit();
      }
      catch (Exception e)
      {
        conexion.rollback();
        throw new Exception(e.getMessage());
      }
      finally
      {
        conexion.setAutoCommit(true);
        conexion.close();
      }
      return Response.ok(j.writeValueAsString(new Respuesta("Se borró el usuario"))).build();
    }
    catch (Exception e)
    {
      return Response.status(400).entity(j.writeValueAsString(new Respuesta(e.getMessage()))).build();
    }
  }


//Nuevo codigo----------------------------------------------

// RF-BE-1: Alta de artículo
  @POST
  @Path("alta_articulo")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response alta_articulo(@QueryParam("id_usuario") int id_usuario, @QueryParam("token") String token, Articulo articulo) throws Exception {
    try {
      Connection conexion = pool.getConnection();
      if (!verifica_acceso(conexion, id_usuario, token))
        return Response.status(400).entity(j.writeValueAsString(new Respuesta("Acceso denegado"))).build();

      conexion.setAutoCommit(false);
      try {
        PreparedStatement stmt_1 = conexion.prepareStatement("INSERT INTO stock(nombre, descripcion, precio, cantidad) VALUES (?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
        stmt_1.setString(1, articulo.nombre);
        stmt_1.setString(2, articulo.descripcion);
        stmt_1.setDouble(3, articulo.precio);
        stmt_1.setInt(4, articulo.cantidad);
        stmt_1.executeUpdate();

        ResultSet rs = stmt_1.getGeneratedKeys();
        int id_articulo = 0;
        if (rs.next()) id_articulo = rs.getInt(1);
        rs.close();
        stmt_1.close();

        if (articulo.foto != null) {
          PreparedStatement stmt_2 = conexion.prepareStatement("INSERT INTO fotos_articulos(foto, id_articulo) VALUES (?,?)");
          stmt_2.setBytes(1, articulo.foto);
          stmt_2.setInt(2, id_articulo);
          stmt_2.executeUpdate();
          stmt_2.close();
        }
        conexion.commit();
        return Response.ok(j.writeValueAsString(new Respuesta("OK"))).build();
      } catch (Exception e) {
        conexion.rollback();
        throw new Exception(e.getMessage());
      } finally {
        conexion.setAutoCommit(true);
        conexion.close();
      }
    } catch (Exception e) {
      return Response.status(400).entity(j.writeValueAsString(new Respuesta(e.getMessage()))).build();
    }
  }

  // RF-BE-2: Consulta de artículos
  @GET
  @Path("consulta_articulos")
  @Produces(MediaType.APPLICATION_JSON)
  public Response consulta_articulos(@QueryParam("id_usuario") int id_usuario, @QueryParam("token") String token, @QueryParam("keyword") String keyword) throws Exception {
    try {
      Connection conexion = pool.getConnection();
      if (!verifica_acceso(conexion, id_usuario, token))
        return Response.status(400).entity(j.writeValueAsString(new Respuesta("Acceso denegado"))).build();

      ArrayList<Articulo> lista = new ArrayList<>();
      PreparedStatement stmt = conexion.prepareStatement("SELECT a.id_articulo, a.nombre, a.descripcion, a.precio, a.cantidad, b.foto FROM stock a LEFT JOIN fotos_articulos b ON a.id_articulo = b.id_articulo WHERE a.nombre LIKE ? OR a.descripcion LIKE ?");
      String paramBusqueda = "%" + keyword + "%";
      stmt.setString(1, paramBusqueda);
      stmt.setString(2, paramBusqueda);
      ResultSet rs = stmt.executeQuery();

      while (rs.next()) {
        Articulo a = new Articulo();
        a.id_articulo = rs.getInt(1);
        a.nombre = rs.getString(2);
        a.descripcion = rs.getString(3);
        a.precio = rs.getDouble(4);
        a.cantidad = rs.getInt(5);
        a.foto = rs.getBytes(6);
        lista.add(a);
      }
      rs.close();
      stmt.close();
      conexion.close();
      return Response.ok(j.writeValueAsString(lista)).build();
    } catch (Exception e) {
      return Response.status(400).entity(j.writeValueAsString(new Respuesta(e.getMessage()))).build();
    }
  }

  // RF-BE-3: Compra de artículo
  @PUT
  @Path("compra_articulo")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response compra_articulo(@QueryParam("id_usuario") int id_usuario, @QueryParam("token") String token, Compra compra) throws Exception {
    try {
      Connection conexion = pool.getConnection();
      if (!verifica_acceso(conexion, id_usuario, token))
        return Response.status(400).entity(j.writeValueAsString(new Respuesta("Acceso denegado"))).build();

      conexion.setAutoCommit(false);
      try {
        // Verificar stock
        PreparedStatement stmt_stock = conexion.prepareStatement("SELECT cantidad FROM stock WHERE id_articulo = ? FOR UPDATE");
        stmt_stock.setInt(1, compra.id_articulo);
        ResultSet rs_stock = stmt_stock.executeQuery();
        if (!rs_stock.next()) throw new Exception("Artículo no encontrado");
        int stock_actual = rs_stock.getInt(1);
        rs_stock.close();
        stmt_stock.close();

        if (compra.cantidad > stock_actual) {
          conexion.rollback();
          return Response.status(400).entity(j.writeValueAsString(new Respuesta("No hay suficientes artículos en el stock"))).build();
        }

        // Restar del stock
        PreparedStatement stmt_upd_stock = conexion.prepareStatement("UPDATE stock SET cantidad = cantidad - ? WHERE id_articulo = ?");
        stmt_upd_stock.setInt(1, compra.cantidad);
        stmt_upd_stock.setInt(2, compra.id_articulo);
        stmt_upd_stock.executeUpdate();
        stmt_upd_stock.close();

        // Verificar si ya está en carrito
        PreparedStatement stmt_check_cart = conexion.prepareStatement("SELECT 1 FROM carrito_compra WHERE id_usuario = ? AND id_articulo = ?");
        stmt_check_cart.setInt(1, id_usuario);
        stmt_check_cart.setInt(2, compra.id_articulo);
        ResultSet rs_cart = stmt_check_cart.executeQuery();
        boolean existeEnCarrito = rs_cart.next();
        rs_cart.close();
        stmt_check_cart.close();

        if (existeEnCarrito) {
          PreparedStatement stmt_upd_cart = conexion.prepareStatement("UPDATE carrito_compra SET cantidad = cantidad + ? WHERE id_usuario = ? AND id_articulo = ?");
          stmt_upd_cart.setInt(1, compra.cantidad);
          stmt_upd_cart.setInt(2, id_usuario);
          stmt_upd_cart.setInt(3, compra.id_articulo);
          stmt_upd_cart.executeUpdate();
          stmt_upd_cart.close();
        } else {
          PreparedStatement stmt_ins_cart = conexion.prepareStatement("INSERT INTO carrito_compra(id_usuario, id_articulo, cantidad) VALUES (?,?,?)");
          stmt_ins_cart.setInt(1, id_usuario);
          stmt_ins_cart.setInt(2, compra.id_articulo);
          stmt_ins_cart.setInt(3, compra.cantidad);
          stmt_ins_cart.executeUpdate();
          stmt_ins_cart.close();
        }

        conexion.commit();
        return Response.ok(j.writeValueAsString(new Respuesta("OK"))).build();
      } catch (Exception e) {
        conexion.rollback();
        throw new Exception(e.getMessage());
      } finally {
        conexion.setAutoCommit(true);
        conexion.close();
      }
    } catch (Exception e) {
      return Response.status(400).entity(j.writeValueAsString(new Respuesta(e.getMessage()))).build();
    }
  }

  // RF-BE-4: Elimina un artículo del carrito
  @DELETE
  @Path("elimina_articulo_carrito_compra")
  @Produces(MediaType.APPLICATION_JSON)
  public Response elimina_articulo_carrito_compra(@QueryParam("id_usuario") int id_usuario, @QueryParam("token") String token, @QueryParam("id_articulo") int id_articulo) throws Exception {
    try {
      Connection conexion = pool.getConnection();
      if (!verifica_acceso(conexion, id_usuario, token))
        return Response.status(400).entity(j.writeValueAsString(new Respuesta("Acceso denegado"))).build();

      conexion.setAutoCommit(false);
      try {
        // Obtener cantidad a devolver al stock
        PreparedStatement stmt_get = conexion.prepareStatement("SELECT cantidad FROM carrito_compra WHERE id_usuario = ? AND id_articulo = ?");
        stmt_get.setInt(1, id_usuario);
        stmt_get.setInt(2, id_articulo);
        ResultSet rs = stmt_get.executeQuery();
        if (!rs.next()) throw new Exception("Artículo no encontrado en el carrito");
        int cantidad_a_devolver = rs.getInt(1);
        rs.close();
        stmt_get.close();

        // Devolver al stock
        PreparedStatement stmt_upd = conexion.prepareStatement("UPDATE stock SET cantidad = cantidad + ? WHERE id_articulo = ?");
        stmt_upd.setInt(1, cantidad_a_devolver);
        stmt_upd.setInt(2, id_articulo);
        stmt_upd.executeUpdate();
        stmt_upd.close();

        // Eliminar del carrito
        PreparedStatement stmt_del = conexion.prepareStatement("DELETE FROM carrito_compra WHERE id_usuario = ? AND id_articulo = ?");
        stmt_del.setInt(1, id_usuario);
        stmt_del.setInt(2, id_articulo);
        stmt_del.executeUpdate();
        stmt_del.close();

        conexion.commit();
        return Response.ok(j.writeValueAsString(new Respuesta("OK"))).build();
      } catch (Exception e) {
        conexion.rollback();
        throw new Exception(e.getMessage());
      } finally {
        conexion.setAutoCommit(true);
        conexion.close();
      }
    } catch (Exception e) {
      return Response.status(400).entity(j.writeValueAsString(new Respuesta(e.getMessage()))).build();
    }
  }

  // RF-BE-5: Elimina todo el carrito
  @DELETE
  @Path("elimina_carrito_compra")
  @Produces(MediaType.APPLICATION_JSON)
  public Response elimina_carrito_compra(@QueryParam("id_usuario") int id_usuario, @QueryParam("token") String token) throws Exception {
    try {
      Connection conexion = pool.getConnection();
      if (!verifica_acceso(conexion, id_usuario, token))
        return Response.status(400).entity(j.writeValueAsString(new Respuesta("Acceso denegado"))).build();

      conexion.setAutoCommit(false);
      try {
        // Obtener todos los artículos del carrito del usuario
        PreparedStatement stmt_get = conexion.prepareStatement("SELECT id_articulo, cantidad FROM carrito_compra WHERE id_usuario = ?");
        stmt_get.setInt(1, id_usuario);
        ResultSet rs = stmt_get.executeQuery();
        
        PreparedStatement stmt_upd = conexion.prepareStatement("UPDATE stock SET cantidad = cantidad + ? WHERE id_articulo = ?");
        while (rs.next()) {
            stmt_upd.setInt(1, rs.getInt(2)); // cantidad
            stmt_upd.setInt(2, rs.getInt(1)); // id_articulo
            stmt_upd.executeUpdate();
        }
        rs.close();
        stmt_get.close();
        stmt_upd.close();

        // Borrar el carrito
        PreparedStatement stmt_del = conexion.prepareStatement("DELETE FROM carrito_compra WHERE id_usuario = ?");
        stmt_del.setInt(1, id_usuario);
        stmt_del.executeUpdate();
        stmt_del.close();

        conexion.commit();
        return Response.ok(j.writeValueAsString(new Respuesta("OK"))).build();
      } catch (Exception e) {
        conexion.rollback();
        throw new Exception(e.getMessage());
      } finally {
        conexion.setAutoCommit(true);
        conexion.close();
      }
    } catch (Exception e) {
      return Response.status(400).entity(j.writeValueAsString(new Respuesta(e.getMessage()))).build();
    }
  }


}
