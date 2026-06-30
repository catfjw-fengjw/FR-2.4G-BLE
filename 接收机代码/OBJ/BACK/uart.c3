

#include	"uart.h"



//==========UART======================
UART_EN_FG		=>	1;			//UART 使能  0：失能 1：使能


UART_Clock		=>	8000000;			//UART时钟，选择1M、2M、4M、8M，其他值默认1M，若使用其他值请咨询FAE
FPPA_Duty		=>	_SYS(INC.FPPA_NUM);	// Single FPPA = 1, Mult FPPA = 2 or 4/8/...

Baud_Rate		=>	38400;				//波特率
UART_Out		BIT	PA.3;				//发送端口

/*
UART_Delay		=>	(UART_Clock / FPPA_Duty) / Baud_Rate;

	if	UART_Clock	=	8,000.000 Hz
		FPPA_Duty		=	/16
	so	FPPA_Clock		=	UART_Clock / FPPA_Duty	=	500.000	Hz

	if	Baud_Rate		=	19200
	so	UART_Delay		=	500.000 / 19200 = 26.0416...
	so	match, 26 cycles send one bit.	< 0.5%

	if	Baud_Rate		=	38400
	so	UART_Delay		=	500.000 / 38400 = 13.02083...
	so	match, 13 cycles send one bit.	< 0.5%

	if	Baud_Rate		=	56000
?	so	UART_Delay		=	500.000 / 56000 = 8.9285...		<	1.0%

	if	Baud_Rate		=	57600
X	so	UART_Delay		=	500.000 / 57600 = 8.6805...		:	fail
*/
UART_Delay		=>	( (UART_Clock / FPPA_Duty) + (Baud_Rate/2) ) / Baud_Rate;
//	+ (Baud_Rate/2) : to round up or down
Test_V0			=>	UART_Clock / 1000 * 995;
Test_V1			=>	UART_Delay * Baud_Rate * FPPA_Duty;
Test_V2			=>	UART_Clock / 1000 * 1005;

#if	(Test_V1 < Test_V0) || (Test_V1 > Test_V2)
	.echo	%Test_V0 <= %Test_V1 <= %Test_V2
	.error	Baud_Rate do not match to System Clock
#endif


byte	SYS_CLKMD;
byte	CLKMD_BK;

void	Clock_Adjust(void)//时钟调整
{
	#if		UART_Clock == 8000000
	{
		CLKMD_BK	=	0x34;//8M
	}
	#elseif	UART_Clock == 4000000
	{
		CLKMD_BK	=	0x14;//4M
	}
	#elseif	UART_Clock == 2000000
	{
		CLKMD_BK	=	0x3c;//2M
	}
	#else
	{
		UART_Clock	=>	1000000
		CLKMD_BK	=	0x1c;//1M
	}
	#endif
	CLKMD = CLKMD_BK;//将系统时钟修改为设定的UART时钟
	nop;//等待
}


//发送程序
static	void	UART_Send (void)
{
	BYTE	cnt;
	BYTE	UART_Data_Out;

	Disgint;

	UART_Data_Out	=	A;

	//	Start Bit
	set0	UART_Out;				//	1

	#if	FPPA_Duty == 1
		cnt	=	8;						//	2 ~ 3
		.Delay	3;						//	4 ~ 6
		do
		{	//	Data Bit * 8
			.Delay	UART_Delay - 10;
			sr		UART_Data_Out;		//	7
			if (CF)
			{
				nop;					//	10
				UART_Out	=	1;		//	1
			}
			else
			{
				UART_Out	=	0;		//	1
				.delay	2;				//	2 ~ 3
			}
		} while (--cnt);				//	4 ~ 6
		.Delay	UART_Delay - 5;
	#else
		.Delay	UART_Delay - 4;
		cnt	=	8;						//	2 ~ 3

		//	Data Bit * 8
		do
		{
			sr		UART_Data_Out;		//	4		4
			swapc	UART_Out;			//			1
			.Delay	UART_Delay - 4;
		} while (--cnt);				//			2, 3

		.Delay	2;						//			3 ~ 4
	#endif

	//	Stop Bit
	set1	UART_Out;				//	1
	.Delay	2 * UART_Delay - 2;

	Engint;
}									//	2

void	UART_HandShake (void)
{
	Clock_Adjust();		//将系统时钟修改为设定的UART时钟

	if(Uart_Send_Fg)
	{
		Uart_Send_Fg = 0;
		A = num >> 8;		//发送一个byte
	}
	else
	{
		Uart_Send_Fg = 1;
		A =	num & 0xFF;		//发送一个byte
	}


//	byte num = 0x66;
	UART_Send();		//发送数据
	CLKMD = SYS_CLKMD;	//数据发送结束后，切回原来的系统时钟
	nop;//等待


}
