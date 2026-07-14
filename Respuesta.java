/*
  Respuesta.java
  Permite regresar al cliente REST un mensaje
*/

package servicio;

public class Respuesta
{
	public String mensaje;

	Respuesta(String mensaje)
	{
		this.mensaje = mensaje;
	}
}
